Github packages includes a bad version of this jar, I don't know why.
it exists as both:
  com/github/umjammer/vavi-commons/1.1.10/vavi-commons-1.1.10.jar
  and
  com/githum/umjammer/vavi-commons/vavi-commons/1.1.10/vavi-commons-1.1.10.jar

The first one is broken (empty jar file).
The second one works.

Weirdly, when I build and run from within intellij, it finds the "right" one and everything is fine.
But when I build with maven on the command line, it finds the "wrong" one and it breaks.
The symptom is a ClassNotFound exception at runtime looking for vavi.Debug in that jar.

I messed around with maven quite a lot trying to find a solution to this.
In the end, the only way I could make it consistently work is to
add code to my pom.xml to manually copy the "good" jar into the lib directory after the build.
This is stupid and shouldn't be necessary, but here we are.


