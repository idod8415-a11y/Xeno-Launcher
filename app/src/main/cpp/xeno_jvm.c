#include <jni.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <android/log.h>

#define TAG "Xeno"

typedef int (*JLI_Launch_t)(int argc, char **argv,
                            int jargc, const char **jargv,
                            int appclassc, const char **appclassv,
                            const char *fullversion, const char *dotversion,
                            const char *pname, const char *lname,
                            unsigned char javaargs, unsigned char cpwildcard,
                            unsigned char javaw, int ergo);

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

    /* stdout и stderr уходят в лог-файл, его читает лаунчер */
    int fd = open(logPath, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd >= 0) {
        dup2(fd, 1);
        dup2(fd, 2);
        if (fd > 2) close(fd);
    }
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);

    setenv("JAVA_HOME", home, 1);
    setenv("LD_LIBRARY_PATH", ld, 1);

    fprintf(stderr, "[xeno] JAVA_HOME=%s\n", home);
    fprintf(stderr, "[xeno] libjvm=%s\n", jvmPath);
    fprintf(stderr, "[xeno] libjli=%s\n", jliPath);

    /* Android не учитывает LD_LIBRARY_PATH после старта процесса,
       поэтому грузим главные библиотеки по полному пути */
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

    int n = (*env)->GetArrayLength(env, jArgs);
    char **argv = calloc(n + 2, sizeof(char *));
    argv[0] = strdup("java");
    for (int i = 0; i < n; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, jArgs, i);
        argv[i + 1] = dupstr(env, s);
        (*env)->DeleteLocalRef(env, s);
    }
    for (int i = 0; i <= n; i++) {
        fprintf(stderr, "[xeno] arg[%d]=%s\n", i, argv[i]);
    }

    int rc = launch(n + 1, argv, 0, NULL, 0, NULL,
                    "21-internal", "21", "java", "openjdk",
                    0, 1, 0, 1);
    fprintf(stderr, "[xeno] JLI_Launch returned %d\n", rc);
    return rc;
}
