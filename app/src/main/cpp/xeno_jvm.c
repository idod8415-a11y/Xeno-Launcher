#include <jni.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef int (*JLI_Launch_t)(int argc, char **argv,
                            int jargc, const char **jargv,
                            int appclassc, const char **appclassv,
                            const char *fullversion, const char *dotversion,
                            const char *pname, const char *lname,
                            unsigned char javaargs, unsigned char cpwildcard,
                            unsigned char javaw, int ergo);

static void logmsg(const char *m) { write(2, m, strlen(m)); }

static void on_exit_cb(void) { logmsg("[xeno] exit() was called\n"); }

static void on_signal(int sig) {
    char buf[64];
    int n = snprintf(buf, sizeof buf, "[xeno] CRASH signal %d\n", sig);
    write(2, buf, n);
    signal(sig, SIG_DFL);
    raise(sig);
}

static char *dupstr(JNIEnv *env, jstring s) {
    const char *c = (*env)->GetStringUTFChars(env, s, NULL);
    char *r = strdup(c);
    (*env)->ReleaseStringUTFChars(env, s, c);
    return r;
}

JNIEXPORT jint JNICALL
Java_com_xeno_launcher_Native_launch(JNIEnv *env, jclass cls,
                                     jstring jHome, jstring jLd,
                                     jstring jJvm, jstring jJli,
                                     jstring jLog, jobjectArray jArgs) {
    char *home = dupstr(env, jHome);
    char *ld = dupstr(env, jLd);
    char *jvmPath = dupstr(env, jJvm);
    char *jliPath = dupstr(env, jJli);
    char *logPath = dupstr(env, jLog);

    int fd = open(logPath, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd >= 0) {
        dup2(fd, 1);
        dup2(fd, 2);
        if (fd > 2) close(fd);
    }
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);

    atexit(on_exit_cb);
    signal(SIGSEGV, on_signal);
    signal(SIGABRT, on_signal);
    signal(SIGBUS, on_signal);
    signal(SIGILL, on_signal);

    setenv("JAVA_HOME", home, 1);
    setenv("LD_LIBRARY_PATH", ld, 1);
    setenv("_JAVA_LAUNCHER_DEBUG", "1", 1);

    fprintf(stderr, "[xeno] pid=%d\n", (int) getpid());
    fprintf(stderr, "[xeno] JAVA_HOME=%s\n", home);
    fprintf(stderr, "[xeno] LD_LIBRARY_PATH=%s\n", ld);

    void *jvm = dlopen(jvmPath, RTLD_NOW | RTLD_GLOBAL);
    if (!jvm) {
        fprintf(stderr, "[xeno] ERROR dlopen libjvm: %s\n", dlerror());
        return -1;
    }
    void *jli = dlopen(jliPath, RTLD_NOW | RTLD_GLOBAL);
    if (!jli) {
        fprintf(stderr, "[xeno] ERROR dlopen libjli: %s\n", dlerror());
        return -2;
    }
    JLI_Launch_t launch = (JLI_Launch_t) dlsym(jli, "JLI_Launch");
    if (!launch) {
        fprintf(stderr, "[xeno] ERROR dlsym JLI_Launch: %s\n", dlerror());
        return -3;
    }
    fprintf(stderr, "[xeno] libs loaded, calling JLI_Launch\n");

    int n = (*env)->GetArrayLength(env, jArgs);
    char **argv = calloc(n + 2, sizeof(char *));
    argv[0] = strdup("java");
    for (int i = 0; i < n; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, jArgs, i);
        argv[i + 1] = dupstr(env, s);
        (*env)->DeleteLocalRef(env, s);
    }

    int rc = launch(n + 1, argv, 0, NULL, 0, NULL,
                    "21-internal", "21", "java", "openjdk",
                    0, 1, 0, 1);
    fprintf(stderr, "[xeno] JLI_Launch returned %d\n", rc);
    return rc;
}
