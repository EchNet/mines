# mines

My very first Java applet

V3 comes with a nifty web interface written using CSS, DHTML and Javascript.

Upgrades to Java platform and in hardware exposed some bugs in the applet.
Bugs fixed:
- Cascading repaint method of updating the timer was pinning the CPU!  
  Replaced it with the more traditional approach of an independent Thread.

- There was no repaint immediately after first cell was clicked.

- Occasional red bleed from bomb cell into successive squares.

- Fixed some problems related to mouse click handling:

- If two right clicks in cell with no mouse move in between, 2nd click acts like double click.

- Double click only works if left click falls first.

## known bugs
consider introducing delay of activation of right click (rotate) - if left falls first, it's a double click.
flicker on page resize in FireFox.
restore game parameters on return to page ?
fix some of the screenshots on the instructions page
