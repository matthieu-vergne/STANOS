# Description

## Context

I have played a bit with [STAN](http://stan4j.com/), a structure analysis software which can be used as a standalone or Eclipse plugin.
I found it particularly nice, with great data presentation and an easy to use interface.

## Problem

However, it has now several drawbacks:
- it ignores lambdas, which are more and more in use, so more and more useful dependencies information get lost, making the tool less reliable.
- it is not available on Linux, and although using it through wine works it does have some weird behaviours impairing the user experience.
- it is not open source, and not being able to dig in it to propose fixes and improvements makes it particulary frustrating.
- it is not maintained anymore, or at least I failed in convincing the author in supporting lambdas, which are here to stay (and the help page show that most of the modules it contains date from 2011-2013, except the STAN stuff which is from 2017)

Wrapping it up, I am limited by the lack of lambda supports (which I use more and more heavily) and it makes me worry about the support of Java 9+, with all the new module stuff.

## Solution

This project comes as an open source alternative to STAN, thus the name: STANOS.
Yeah, I know it lacks in creativity, but STAN stands for STructure ANalysis, so let's be homogeneous.

I don't plan to reverse engineer STAN, although not only because of legal issues.
It is also more interesting, fulfilling, and teaching to search for my own way to do stuff.
I also think that it makes no sense to inspire from some old code when a lot of new stuff is coming in the Java world.
So the  whole point is to see what STAN provides, identify the interesting bits, and try to do something even better here, all in open source.

# How does it Work?

[TODO]

# How to use it?

[TODO]
