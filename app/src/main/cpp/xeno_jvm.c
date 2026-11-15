#include <jni.h>
#include <dirent.h>
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

#define MAXLIBS 256
static char *libs[MAXLIBS];
static int nlibs = 0;

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

static void collect(const char *dir) {
    DIR *d = opendir(dir);
    if (!d) return;
    struct dirent *e;
    while ((e = readdir(d)) != NULL && nlibs < MAXLIBS) {
        size_t l = strlen(e->d_name);
        if (l > 3 && strcmp(e->d_name + l - 3, ".so") == 0
            && strstr(e->d_name, "jsig") == NULL) {
            char *p = malloc(strlen(dir) + l + 2);
            sprintf(p, "%s/%s", dir, e->d_name);
            libs[nlibs++] = p;
        }
    }
    closedir(d);
}

static void preload_all(const char *home) {
    char dir[512];
    snprintf(dir, sizeof dir, "%s/lib", home);
    collect(dir);
    snprintf(dir, sizeof dir, "%s/lib/server", home);
    collect(dir);

    int loaded[MAXLIBS];
    memset(loaded, 0, sizeof loaded);
    int total = 0;
    for (int pass = 0; pass < 6; pass++) {
        int progress = 0;
        for (int i = 0; i < nlibs; i++) {
            if (loaded[i]) continue;
            if (dlopen(libs[i], RTLD_NOW | RTLD_GLOBAL)) {
                loaded[i] = 1;
                progress = 1;
                total++;
            }
        }
        if (!progress) break;
    }
    fprintf(stderr, "[xeno] preloaded %d of %d libs\n", total, nlibs);
    for (int i = 0; i < nlibs; i++) {
        if (!loaded[i]) fprintf(stderr, "[xeno] not loaded: %s\n", libs[i]);
    }
}

JNIEXPORT jint JNICALL
Java_com_xeno_launcher_Native_launch(JNIEnv *env, jclass cls,
                                     jstring jHome, jstring jLd,
                                     jstring jJvm, jstring jJli,
                                     jstring jLog, jobjectArray jArgs) {
    char *home = dupstr(env, jHome);
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
    unsetenv("LD_LIBRARY_PATH");

    fprintf(stderr, "[xeno] pid=%d\n", (int) getpid());
    fprintf(stderr, "[xeno] JAVA_HOME=%s\n", home);

    const char *cwd = getenv("XENO_CWD");
    if (cwd != NULL) {
        if (chdir(cwd) == 0) {
            fprintf(stderr, "[xeno] cwd=%s\n", cwd);
        } else {
            fprintf(stderr, "[xeno] chdir failed: %s\n", cwd);
        }
    }

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
    preload_all(home);
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
