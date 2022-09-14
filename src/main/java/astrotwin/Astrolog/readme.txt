 AAAAA    SSSSS   TTTTTTT  RRRRRR    OOOOO   L         OOOOO    GGGGG
A     A  S     S     T     R     R  O     O  L        O     O  G     G
A     A  S           T     R     R  O     O  L        O     O  G
AAAAAAA   SSSSS      T     RRRRRR   O     O  L        O     O  G  GGGG
A     A        S     T     R   R    O     O  L        O     O  G     G
A     A  S     S     T     R    R   O     O  L        O     O  G     G
A     A   SSSSS      T     R     R   OOOOO   LLLLLLL   OOOOO    GGGGG
 
README for Astrolog version 7.40 (March 2022), compiled for MacOS
=================================================================

This readme.txt explains how to run astrolog in the MacOS ecosystem using
pre-compiled executable binaries. It also offers a step by step guide for
compiling your own executable binary from the included source code, if you
don't want to wait for the new binaries to become available when the next
version of astrolog drops, or if you're interested in programming or
working with source code on a mac.

BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR
THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED
OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM
PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR
CORRECTION.

IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL
ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE
THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY
GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE
USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF
DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD
PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS),
EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGES. 

ROADMAP 
=======

OVERVIEW                What it is, what it isn't, and what it needs.
FIRST RUN               About the unidentified developer warning.
RUNNING ASTROLOG        "astrolog" is a standalone command line executable.
RUNNING ASTROLOGX11     "astrologx11" is a dependent command line executable
                          providing limited display interactivity through
                          an "X Windows System" (a.k.a. X11) software port.
INSTALLING X11/XQUARTZ  ...to satisfy the astrologx11 library dependency.
COMPILING FROM SOURCE   Make your own astrolog executable from source code.
SIGNING THE EXECUTABLE  ...if your compiled executable won't run, or if you
                          want to get rid of the MacOS error: -67062 message
                          that may show up in the Consols.app.

OVERVIEW
========

How is this different from the Windows version of Astrolog?
-----------------------------------------------------------

Congratulations on finding this pre-compiled distribution of Astrolog for
Macintosh. The main difference from the Windows version is that it does not
offer a Graphical User Interface, and so you will not find a mouse-driven
menu system or dialog interface. However, it offers just about everything
else. You can even draw temporary squiggles/annotations with your mouse on
a chart displayed in X11.

I need to become familiar with the macOS command line terminal to run this?
---------------------------------------------------------------------------

The Macintosh versions of Astrolog (called astrolog and astrologx11) have
command line interfaces, so they're designed to always be run from a
terminal program such as Terminal.app. You have to specify command switches
for the settings you want (even graphics settings) and the standard
scenario is that a separate graphics window only pop up if you specify the
right graphics switches from the terminal (i.e. "-X").

I can't just double click an executable from the Finder?
--------------------------------------------------------

Neither astrolog nor astrologx11 are designed to ever be opened by double
clicking. If you double click the executable, depending on how your system
is configured, macOS may spawn a Terminal.app window, run the program in
that window, and immediately terminate in the blink of an eye. Or it may
simply allow you to interact with the program as it would if you were to
enter the program name from the command line yourself. Your mileage may
vary because many configuration options are available to you on a modern
mac. It's possible to configure a shortcut to, for example, start Astrolog
as a background process showing an animated graphics window (and close its
launching window).

FIRST RUN
=========

Running for the first time
--------------------------

Open Terminal.app, change directories to where you've extracted the
distribution, and enter astrolog or astrologx11 from the command line.
Hold that thought...

What's all this about an "unidentified developer"?
--------------------------------------------------

Compiled on macOS 10.14, astrolog and astrologx11 are binary executables
that have been signed with an anonymous, self-signed code signing
certificate. For this reason you can expect to see a popup window when
trying to run them the first time: i.e. "astrolog can't be opened because
it is from an unidentified developer." When this happens, you can go into
System Preferences -> Security & Privacy and look for:

     "astrolog" was blocked from opening because it
     is not from an identified developer.

Click "Open Anyway", and click "Open" again on the following dialog:

     "astrolog" can't be opened because the identity
     of the developer cannot be confirmed. Opening
     astrolog will always allow it to run on this Mac.

This only needs to be done once per astrolog executable. If any of this
bothers or worries you, consider using Apple's free developer tools to
compile your own executable from the Astrolog source code. It's really fun
if you're into that sort of thing; Read the primer below to get started.

RUNNING ASTROLOG
================

The most common way to run an astrolog executable is to open Terminal.app,
change directories to where you've extracted the distribution, and enter
"astrolog" or "astrologx11" from the command line. You may also configure a
Terminal.app profile to do this for you, and more. For more information
about setting the PATH variable in macOS, and for configuring Terminal.app
profiles, search the internet. Consult the official Astrolog documentation
for further configuration and usage guidelines for Astrolog itself.

The X11 version, "astrologx11", is identical to the non-X11 version,
"astrolog", in that both binaries can both produce the same text mode
charts in the Terminal and can both generate bitmap graphics files on your
file system. The main differences are:

   1) "astrologx11" also has the option to spawn a separate graphics window
      instead of only being able to save graphics charts to file, which
      must then be viewed in another program (mentioned below).

   2) The X11 graphics window spawned by "astrologx11" accepts key presses
      and mouse input to perform functions, change settings, and modify or
      completely change its graphical display content. The Terminal window
      remains open to display text based information such as current chart
      position data, or a list of keyboard and mouse options (shown when 
      typing ?).

What do the two executables have in common?
-------------------------------------------

Both astrolog executables ("astrolog" and "astrologx11") provide text
output (even in color if configured to do so), and can generate graphics
output files (like Windows style bitmap files, for example: astrolog -n
-Xbw -Xo chart.bmp), but the non-X11 version ("astrolog") is not designed
to automatically open a popup window displaying chart graphics. However, if
you generated a chart.bmp file, mentioned above, you can still open it from
the command line using the built in macOS command, i.e. "open chart.bmp".

What the X11 support offers
---------------------------

So, the X11 version of astrolog ("astrologx11") is not required to output
graphics files. But it is required if you want to change how a graphic
chart appears in an X11 window, using key press options. For instance, you
can change from a normal 2D chart to a 3D chart sphere by typing X, or to a
world map by typing W. Type ? to see the full list of graphics screen key
press options. The help will be displayed in your Terminal window while the
graphic chart is displayed in a separate X11 window. Note: The X11 window
needs to have the focus for Astrolog to properly interpret key presses; If
you press a key while your Terminal has focus, the character will just show
up uselessly in the terminal window, and when you terminate the program,
your shell will try to process what you typed: Note that this is a feature
of Unix shells and their buffered input, not Astrolog, and could lead to
surprising or unexpected output like "command not found".

ASTROLOGX11 DEPENDENCY: X11/XQUARTZ
===================================

To see popup graphics windows using astrologx11, a compatible X11 library
is required, such the XQuartz project. As of the time of this writing, you
can get the latest version from https://www.xquartz.org/. XQuartz 2.8.1 has
been tested to work with astrologx11 v7.40 in macOS 10.14.

After installation, run the XQuartz application. Once it is running, you
can run astrologx11 with an -X switch (i.e. astrologx11 -X), and an X11
window will spawn automatically while your Terminal remains open, and the
X11 window will have the focus. You can press Escape to quit the program,
or type ? to get a list of interactive commands. If you give focus back to
the Terminal, you can control-C to force quit. If you press Enter within
within the X11 window, it will cause the Terminal window to show a prompt
for Astrolog command switches, and you'll need to switch focus to supply
the input and switch focus back to the X11 window to continue interacting
with it.


COMPILING FROM SOURCE
=====================
Learn to compile astrolog from the source code

"Command Line Tools"
--------------------

To compile an astrolog executable from the source code, your operating
system needs the familiar unix compiler toolkit, which includes Make, GCC,
LLVM, and other developer components. These tools don't come with macOS by
default, but are available as a free download from Apple. They also become
installed after installing Xcode, but take note that you should not install
Xcode unless you're developing applications for the mac, because it's huge
(requiring over 46 gigabytes at the time of this writing, even though the
App Store says it requires 12.7 gigabytes to download), and contains way
more than you need to compile astrolog. By contrast, Command Line Tools is
a cool 700 megabytes.

Installing "Command Line Tools"
-------------------------------

The simplest way to install Command Line Tools is to open Terminal.app and
type gcc. If Command Line Tools is installed already, the gcc command will
tell you "clang: error: no input files", and that's good news, you don't
have to install it, because it's already there. Otherwise, you will see a
dialog box pop up, saying "The gcc command requires the command line
developer tools. Would you like to install the tools now?", and you can
click the "Install" button. Again, be careful not to click the "Get Xcode"
button unless you really want to install that behemoth. After clicking the
"Install" button, you will be shown the Command Line Tools License
Agreement, for which you can click the "Agree" button to continue. Then,
the software will be downloaded and installed. Finally, you will see a
dialog stating "The software was installed" with a "Done" button that you
can click. Now verify your installation by typing the following command at
your terminal prompt. 

xcode-select -p

You should see the path that Command Line Tools was installed. You can also
see what version of gcc was just installed by running the following
command.

gcc --version

Nice! You're all set to compile the Astrolog source code in the next
section.

If for some reason this installation method doesn't work for your version
of macOS, you can also try the following command at the prompt, it works
the same way as the above method.

xcode-select --install

You should see a dialog box saying "The xcode-select command requires the
command line developer tools. Would you like to install the tools now?",
and click the "Install" button as mentioned above.

If all else fails, you can sign into the Apple developer web site with your
Apple ID and download the Command Line Tools for Xcode from there. Older
versions exist for older versions of macOS.

Are there any bugs to patch before compiling is successful?
-----------------------------------------------------------

Your compilation may stop with errors, and if this happens, first you
should check the page https://www.astrolog.org/astrolog/astnext.htm to see
if there are any known issues.

Configure the codebase to run on your mac
-----------------------------------------

To configure the codebase, you'll need to edit one or more files in the
source directory. Start by editing the header file called astrolog.h, in
the source directory. You can use the vi editor or nano, for example:

nano astrolog.h

Next you'll comment or uncomment a few lines of code to tell the compiler
that you're running on a mac, you're not running on a pc, and you're also
not running in Microsoft Windows.

1) Find the line beginning with "#define PC" and comment it out by prepending
   two slashes like this: "//#define PC".

2) Find the line beginning with "#define WIN" and comment it out the same
   way, like this: "//#define WIN".

3) If you're compiling for the X11 environment, find the line begining with
   "//#define X11", and uncomment it by removing the leading double slashes,
   so it looks like this: "#define X11".

Compiling Astrolog
------------------

This is the easiest step. The following commands will compile the source
code into object files and link those object files together with the
required library or libraries to produce your very own binary executable
file.

For compilation without X11 support, use the following command string and
wait a few seconds for it to complete:

gcc -c -O -Wno-write-strings -Wno-parentheses -Wno-unsequenced -Wno-constant-conversion -Wno-format *.cpp; gcc -o astrolog *.o -lm

For compilation with X11 support, use the following command string and wait
a few seconds for it to complete:

gcc -I /usr/X11/include -c -O -Wno-write-strings -Wno-parentheses -Wno-unsequenced -Wno-constant-conversion -Wno-format *.cpp; gcc -o astrolog *.o -lm -lX11 -L/usr/X11/lib

If everything went well, you'll have a new executable file in the
current directory called "astrolog". A celebration is in order!

SIGNING THE EXECUTABLE
======================

Should I sign the astrolog executable with a digital signature?
---------------------------------------------------------------

In macOS 10.14 you can run the program as is with no visible issues, but in
the Console.app, each time you run astrolog, a macOS process called
taskgated will report "MacOS error: -67062", meaning the executable is not
signed at all. This is just a security policy notification that doesn't
affect runtime of astrolog at all. However, if you so desire, one way of
suppressing the error is to sign the executable.

In macOS 10.15 and above, Apple has tightened the security policy. This may
not be an issue when running without X11 support, but it will certainly be
an issue when running with X11 support. This is because Gatekeeper will
enforce a security policy against improperly signed executables by throwing
an error in your terminal session instead of running the program. In this
case, you can satisfy the security policy by signing the executable and
granting it a software entitlement to access the 3rd party X11 library,
which is signed by an Apple Developer ID for XQuartz.

Preparing a certificate and signing your compiled executable
============================================================
This is way easier than it sounds, just follow the steps.

   __________________________________________________
1) Generate a self signed digital signing certificate.

   a) In the Keychain Access app on your Mac, choose
      Keychain Access > Certificate Assistant > Create a Certificate.
   b) Enter a Name for the certificate, i.e. "My Code Signing Certificate".
   c) Leave the Identity Type set to its default, "Self Signed Root".
   d) Set the Certificate Type to "Code Signing".
   e) Click the Create button.
   f) Click the Continue button.
   g) Review and click the Done button.

   _____________________________________________________________
2) If running with X11 support, create a text file with the name
   "library-validation.xml", and the following content:

<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>com.apple.security.cs.disable-library-validation</key>
  <true/>
</dict>
</plist>

   ______________________________________________________________________
3) If running without X11 support, sign the executable with the following
   command, using the name of your new certificate after the -s switch:

codesign -f -o runtime --timestamp -s "My Code Signing Certificate" ./astrolog

   ___________________________________________________________________
4) If running with X11 support, sign the executable with the following
   command, using the name of your new certificate after the -s switch,
   and the name of your entitlement file after the --entitlements switch:
 
codesign -f -o runtime --timestamp -s "My Code Signing Certificate" ./astrolog --entitlements library-validation.xml

   __________________________________________________________________
5) You can verify the executable is signed with the following command:

codesign -dv -r- ./astrolog

   ______________________________________________________________
6) Optional: If running with X11 support, you can also verify the
   attached entitlement content:

codesign -dv --entitlements :- ./astrolog

   ________________________________________
*) That's it! Enjoy using Astrolog in macOS!
   This readme was authored by Eddie Easterly
