# netty-reproducer

### Expected behavior
When a SSL handshake fails, a channel promise of a write operation should fail aswell. 

### Actual behavior
The channel promise succeeds. 

### Steps to reproduce

    $ git clone https://github.com/s-gheldd/netty-reproducer.git
    $ cd netty-reproducer
    $ mvn test

### Minimal yet complete reproducer code (or URL to code)

https://github.com/s-gheldd/netty-reproducer

### Netty version

`4.1.16.Final`

(last version that had the expected behavior `4.1.13.Final`)

### JVM version (e.g. `java -version`)

    java version "1.7.0_80"
    Java(TM) SE Runtime Environment (build 1.7.0_80-b15)
    Java HotSpot(TM) 64-Bit Server VM (build 24.80-b11, mixed mode)

### OS version (e.g. `uname -a`)

    Darwin ghelds-MacBook-Pro.local 15.6.0 Darwin Kernel Version 15.6.0: Mon Oct  2 22:20:08 PDT 2017; root:xnu-3248.71.4~1/RELEASE_X86_64 x86_64
