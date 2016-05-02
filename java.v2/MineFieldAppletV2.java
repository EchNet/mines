/**
 *  MineFieldApplet.java
 *
 *  Copyright (C) 1996, 1998 by James Echmalian.  All rights reserved.
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.image.*;
import java.util.Random;

/**
 * MineFieldApplet
 * An applet interface to the MineField game.
 * Class is declared final for performance reasons only.
 *
 * @version 2.0
 * @author James Echmalian, ech@ech.net
 */
public final class MineFieldApplet extends Applet
{
    // Applet parameters. 
    private int rows = 8;
    private int columns = 8;
    private int nmines = 10;

    // The Gamekeeper:
    private boolean marksQ;
    private byte[][] tags;
    private byte[][] mined;
    private int exposedCount;
    private int unminedCount;
    private boolean running;

    // Layout variables:
    private Rectangle panelRect;
    private Rectangle topPanelRect;
    private Rectangle gridPanelRect;
    private Rectangle gridRect;
    private Rectangle restartButtonRect;
    private Rectangle counterRect;
    private Rectangle timerRect;

    // Layout constants:
    private static final int cellWidth = 16;
    private static final int cellHeight = 16;
    private static final int gridBorderWidth = 3;
    private static final int gridBorderHeight = 3;
    private static final int panelBorderWidth = 9;
    private static final int panelBorderHeight = 9;
    private static final int panelDividerHeight = 6;
    private static final int topPanelHeight = 37;
    private static final int restartButtonSide = 26;
    private static final int countBoxWidth = 39;

    // Images.
    private Image[] tagImages = new Image [15]; 
    private Image happyImage;
    private Image scaredImage;
    private Image deadImage;
    private Image coolImage;
    private Image[] sevenSegImages = new Image [11];

    // Colors:
    private final Color darkerColor = Color.lightGray.darker ();

    // Graphics control:
    private boolean[][] needUpdate;
    private Rectangle updateArea;
    private Rectangle lastHotArea;
    private boolean restartPressed;
    private Image restartImage;
    private int[] counterDigits;
    private int[] timerDigits;
    private int counter;

    // Timer state:
    private boolean timerRunning;
    private boolean ticking;
    private long accumTime;
    private long startTime;
    private int timer;

    // Mouse event state:
    private int mousex = -1;
    private int mousey = -1;
    private int mouseWeight = 0;
    private int mouseState = MOUSE_COLD;

    // Mouse state constants:
    private static final int MOUSE_COLD = 0;
    private static final int MOUSE_HOT_FOR_CELL = 1;
    private static final int MOUSE_HOT_FOR_CLEARING = 2;
    private static final int MOUSE_HOT_FOR_RESTART = 3;
    private static final int MOUSE_READY_TO_FLAG = 4;

    // Tag constants:
    public static final byte TAG_NULL = 0;      // covered, no tag
    public static final byte TAG_FLAG = 1;      // covered, flagged
    public static final byte TAG_QUES = 2;      // covered, marked '?'
    public static final byte TAG_BOOM = 3;      // exposed, mined
    public static final byte TAG_MINE = 4;      // auto-exposed, mined
    public static final byte TAG_OOPS = 5;      // auto-exposed, wrongly flagged
    public static final byte TAG_ZERO = 6;      // exposed, no adjacent mines
    public static final byte TAG_ONE  = 7;      // exposed, 1 adjacent mine
    public static final byte TAG_TWO  = 8;      // exposed, 2 adjacent mines
    public static final byte TAG_THREE = 9;     // exposed, 3 adjacent mines
    public static final byte TAG_FOUR = 10;     // exposed, 4 adjacent mines
    public static final byte TAG_FIVE = 11;     // exposed, 5 adjacent mines
    public static final byte TAG_SIX  = 12;     // exposed, 6 adjacent mines
    public static final byte TAG_SEVEN = 13;    // exposed, 7 adjacent mines
    public static final byte TAG_EIGHT = 14;    // exposed, 8 adjacent mines

    // The color model
    private final ColorModel colorModel = makeColorModel ();

    private Random random = new Random ();

    //---------------------------------------------------------------------
    // Implementation of Applet methods
    //---------------------------------------------------------------------

    public synchronized void init () 
    {
        //
        // Initialize game parameters through applet parameter interface.
        //
        String rowString = getParameter("rows");
        if (rowString != null)
        {
            rows = Integer.parseInt (rowString);
        }
        String columnString = getParameter("columns");
        if (columnString != null)
        {
            columns = Integer.parseInt (columnString);
        }
        String nminesString = getParameter("nmines");
        if (nminesString != null)
        {
            nmines = Integer.parseInt (nminesString);
        }

        //
        // Build images.
        //
        createImages ();
        
        //
        // Create a gamekeeper.
        //
        this.marksQ = true;
        this.tags = new byte[rows][columns];
        this.mined = new byte[rows][columns];
        setupGame (nmines);

        //
        // Initialize layout.
        //
        panelRect = computePanelRect (rows, columns);
        topPanelRect = computeTopPanelRect (columns);
        gridPanelRect = computeGridPanelRect (rows, columns);
        gridRect = computeGridRect (rows, columns);
        restartButtonRect = computeRestartButtonRect (columns);
        counterRect = getCounterRect ();
        timerRect = getTimerRect ();

        //
        // Create graphical control elements.
        //
        needUpdate = new boolean[rows][columns];
        restartPressed = false;
        restartImage = happyImage;
        counter = nmines;
        counterDigits = new int [3];
        timerDigits = new int [3];

        //
        // Initialize timer state.
        //
        startTime = -1;
        ticking = false;
        timer = 0;

        //
        // Get additional game parameters.
        //
        String marksQString = getParameter ("marksQ");
        if (marksQString != null)
        {
            marksQ = marksQString.equals ("true");
        }

        setBackground (Color.lightGray);
        resize (panelRect.width, panelRect.height);
    }

    public synchronized void start () 
    {
        mousex = -1;
        mousey = -1;
        mouseWeight = 0;
        mouseState = MOUSE_COLD;

        if (timerRunning)
        {
            startTime = System.currentTimeMillis ();
            ticking = true;
        }
    }

    public synchronized void stop () 
    {
        if (timerRunning)
        {
            // Store accumulated time.
            accumTime += System.currentTimeMillis () - startTime;
            ticking = false;
        }
    }

    public synchronized void update (Graphics g) 
    {
        g.setColor (getBackground());

        boolean restartPressed = false;
        Image restartImage = running ? happyImage
                                     : gameIsWon () ? coolImage : deadImage;

        Rectangle hotArea = null;

        //
        // Determine which buttons, if any, are pressed.
        //
        switch (mouseState)
        {
        case MOUSE_HOT_FOR_RESTART:
            if (restartButtonRect.inside (mousex, mousey))
            {
                restartPressed = true;
            }
            break;
        case MOUSE_READY_TO_FLAG:
            if (gridRect.inside (mousex, mousey))
            {
                // Flag a cell
                Point p = scaleToRowColumn (mousex, mousey);
                rotateTagAt (p.y, p.x);
            }
            mouseState = MOUSE_COLD;
            break;
        case MOUSE_HOT_FOR_CELL:
            if (gridRect.inside (mousex, mousey))
            {
                // Single hot cell.
                Point p = scaleToRowColumn (mousex, mousey);
                hotArea = new Rectangle (p.x, p.y, 0, 0);
            }
            restartImage = scaredImage;
            break;
        case MOUSE_HOT_FOR_CLEARING:
            if (gridRect.inside (mousex, mousey))
            {
                // A block of hot cells.
                Point p = scaleToRowColumn (mousex, mousey);
                int x0 = p.x == 0 ? 0 : p.x - 1;
                int y0 = p.y == 0 ? 0 : p.y - 1; 
                int x1 = p.x;
                int y1 = p.y;
                if (++y1 == rows)
                    y1 -= 1;
                if (++x1 == columns)
                    x1 -= 1;
                hotArea = new Rectangle (x0, y0, x1 - x0, y1 - y0);
            }
            restartImage = scaredImage;
            break;
        }

        // Update restart button.
        if (restartPressed != this.restartPressed)
        {
            this.restartPressed = restartPressed;
            this.restartImage = restartImage;
            paintRestartButton (g);
        }
        else if (restartImage != this.restartImage)
        {
            this.restartImage = restartImage;
            paintRestartImage (g);
        }

        //
        // If any cells are newly pressed or newly released, add them
        // to the update area.
        //
        if (hotArea == null || lastHotArea == null || 
            !hotArea.equals (lastHotArea))
        {
            updateRect (lastHotArea);
            updateRect (hotArea);
        }
        lastHotArea = hotArea;

        //
        // Update all cells in update area.
        //
        if (updateArea != null)
        {
            int top = updateArea.y;
            int bottom = updateArea.y + updateArea.height;
            int left = updateArea.x;
            int right = updateArea.x + updateArea.width;
            for (int row = top; row <= bottom; ++row)
            {
                for (int column = left; column <= right; ++column)
                {
                    if (!needUpdate[row][column]) 
                        continue;

                    boolean pressed = false;
                    if (hotArea != null)
                    {
                        // Rectangle.inside() is off by one...
                        int xoff = column - hotArea.x;
                        if (xoff >= 0 && xoff <= hotArea.width)
                        {
                            int yoff = row - hotArea.y;
                            if (yoff >= 0 && yoff <= hotArea.height)
                            {
                                byte tag = tags[row][column];
                                pressed = tag == TAG_NULL || tag == TAG_QUES;
                            }
                        }
                    }
                    paintCell (g, row, column, pressed);

                    needUpdate[row][column] = false;
                }
            }
        }

        updateArea = null;

        // Update the unflagged cell counter...
        drawSevenSeg (g, counterRect, counterDigits, 
                      counter, false);

        // Update the timer...
        if (ticking)
        {
            long time = accumTime + (System.currentTimeMillis () - startTime);
            if (time > 999000)
                time = 999000;
            timer = (int)time / 1000;

            // Check again within a quarter second.
            repaint (250);
        }
        drawSevenSeg (g, timerRect, timerDigits, timer, false);
    }

    public synchronized void paint (Graphics g) 
    {
        g.setColor (getBackground());

        // Draw outer rectangle.
        //
        draw3DRect (g, panelRect, true, 3);

        // Draw rectangle around upper panel.
        //
        draw3DRect (g, topPanelRect, false, 2);

        // Draw rectangle around grid.
        //
        draw3DRect (g, gridPanelRect, false, 3);

        // Draw objects in upper panel.
        //
        paintRestartButton (g);
        drawSevenSeg (g, counterRect, counterDigits, 
                      counter < 0 ? 0 : counter, true);
        drawSevenSeg (g, timerRect, timerDigits, timer, true);

        // Draw the grid itself.
        for (int r = 0; r < rows; ++r)
        {
            for (int c = 0; c < columns; ++c)
            {
                paintCell (g, r, c, false);
            }
        }

        updateArea = null;
    }

    private void updateRect (Rectangle rect)
    {
        if (rect != null)
        {
            int top = rect.y;
            int bottom = rect.y + rect.height;
            int left = rect.x;
            int right = rect.x + rect.width;
            for (int row = top; row <= bottom; ++row)
            {
                for (int column = left; column <= right; ++column)
                {
                    needUpdate[row][column] = true;
                }
            }

            if (updateArea == null)
            {
                updateArea = rect;
            }
            else
            {
                updateArea.add (rect);
            }
        }
    }

    //---------------------------------------------------------------------
    // Event handling.
    //---------------------------------------------------------------------

    /**
     * Handle mouse button down event.
     */
    public boolean mouseDown (Event evt, int x, int y) 
    {
        boolean meta = evt.metaDown () || evt.shiftDown ();

        mousex = x;
        mousey = y;

        int maxWeight = meta ? 2 : 1;
        if (++mouseWeight > maxWeight)
            mouseWeight = maxWeight;

        // Is it on the restart button?
        //
        if (!meta && restartButtonRect.inside (x, y))
        {
            mouseState = MOUSE_HOT_FOR_RESTART;
        }
        else if (!running)
        {
            mouseState = MOUSE_COLD;
        }
        else if (!meta)
        {
            mouseState = MOUSE_HOT_FOR_CELL;
        }
        else if (mouseWeight == 1)
        {
            mouseState = MOUSE_READY_TO_FLAG;
        }
        else
        {
            mouseState = MOUSE_HOT_FOR_CLEARING;
        }

        repaint ();
        return true;
    }

    /**
     * Handle mouse button up event.
     */
    public boolean mouseUp (Event evt, int x, int y) 
    {
        mousex = x;
        mousey = y;

        mouseWeight = (evt.metaDown () || evt.shiftDown ()) ? 1 : 0;

        switch (mouseState)
        {
        case MOUSE_HOT_FOR_RESTART:
            if (mouseWeight == 0 && restartButtonRect.inside (x, y))
            {
                // Restart game
                newGame (nmines);
            }
            break;
        case MOUSE_HOT_FOR_CELL:
            if (running && gridRect.inside (x, y))
            {
                // Clear a cell
                Point p = scaleToRowColumn (x, y);
                exposeCellAt (p.y, p.x);
            }
            break;
        case MOUSE_READY_TO_FLAG:
            if (running && gridRect.inside (x, y))
            {
                // Flag a cell
                Point p = scaleToRowColumn (x, y);
                rotateTagAt (p.y, p.x);
            }
            break;
        case MOUSE_HOT_FOR_CLEARING:
            if (running && gridRect.inside (x, y))
            {
                // Clear surrounding cells
                Point p = scaleToRowColumn (x, y);
                clearAround (p.y, p.x);
            }
            break;
        }

        mouseState = MOUSE_COLD;
        return true;
    }

    /**
     *  Handle mouse move event.
     */
    public boolean mouseMove (Event evt, int x, int y) 
    {
        mousex = x;
        mousey = y;
        mouseWeight = 0;
        mouseState = MOUSE_COLD;
        repaint ();
        return true;
    }

    /**
     *  Handle mouse drag event.
     */
    public boolean mouseDrag (Event evt, int x, int y) 
    {
        mousex = x;
        mousey = y;
        repaint ();
        return true;
    }

    //---------------------------------------------------------------------
    // Game management.
    //---------------------------------------------------------------------

    public void newGame (int nmines)
    {
        for (int row = rows; --row >= 0; )
        {
            for (int column = columns; --column >= 0; )
            {
                mined[row][column] = 0;

                tagCell (row, column, TAG_NULL);
            }
        }

        setupGame (nmines);

        ticking = false;
        accumTime = 0;
        timer = 0;
        counter = nmines;
        repaint ();
    }

    private void tagCell (int row, int column, byte tag)
    {
        if (tags[row][column] != tag)
        {
            tags[row][column] = tag;
            needUpdate[row][column] = true;
            if (updateArea == null)
            {
                updateArea = new Rectangle (column, row, 0, 0);
            }
            else
            {
                updateArea.add (column, row);
            }
        }
    }

    private void startTimer () 
    {
        accumTime = 1000;       // timer goes to 1 on click!
        startTime = System.currentTimeMillis ();
        timerRunning = true;
        ticking = true;
    }

    private void setupGame (int nmines)
    {
        //
        // Determine number of available cells.
        //
        long product = (long) rows * (long) columns;
        int ncells = (int) product;
        if (product != ncells || ncells < 4)
        {
            // Zoicks!
            throw new RuntimeException ();
        }

        //
        // Keep the number of mines reasonable.
        //
        if (nmines >= ncells)
            nmines = ncells - 1;
        if (nmines < 1)
            nmines = 1;

        layMines (nmines, ncells);

        exposedCount = 0;
        unminedCount = ncells - nmines;
        running = true;
    }

    private void layMines (int nmines, int ncells)
    {
        //
        // Lay mines, one at a time.
        //
        while (nmines > 0)
        {
            layOneMine (ncells);
            --nmines;
            --ncells;
        }
    }

    private void layOneMine (int nUnminedCells)
    {
        // Randomly select from among available cells.
        int pos = random.nextInt ();
        if (pos < 0) pos *= -1;
        pos %= nUnminedCells;

        //
        // Go find that cell and mine it.
        //
        for (int row = 0; row < rows; ++row)
        {
            for (int col = 0; col < columns; ++col)
            {
                if (mined[row][col] == 0)
                {
                    if (pos-- == 0)
                    {
                        mined[row][col] = 1;
                        return;
                    }
                }
            }
        }

        // Should not be reached...
        throw new RuntimeException ();
    }

    private boolean cellIsExposed (int row, int column)
    {
        return tags[row][column] >= TAG_BOOM;
    }

    private boolean gameIsWon ()
    {
        return exposedCount == unminedCount;
    }

    public void rotateTagAt (int row, int column)
    {
        if (!running)
            return;

        byte tag = tags[row][column];

        switch (tag)
        {
        case TAG_NULL:
            tag = TAG_FLAG;
            counter -= 1;
            break;
        case TAG_FLAG:
            tag = marksQ ? TAG_QUES : TAG_NULL;
            counter += 1;
            break;
        case TAG_QUES:
            tag = TAG_NULL;
            break;
        default:
            return;     // can't tag exposed cell
        }

        tagCell (row, column, tag);
    }

    public void exposeCellAt (int row, int column)
    {
        if (cellIsExposed (row, column) || tags[row][column] == TAG_FLAG)
            return;

        boolean wasMined = mined[row][column] != 0;

        if (exposedCount == 0 && wasMined)
        {
            // Don't allow the first exposed cell to be mined.
            // Move that mine somewhere else.
            layOneMine (unminedCount);
            mined[row][column] = 0;
            wasMined = false;
        }

        if (wasMined)
        {
            tagCell (row, column, TAG_BOOM);
            showLoss ();
            running = false;
        }
        else
        {
            if (exposedCount == 0)
            {
                startTimer ();
            }
            ++exposedCount;

            rippleExpose (row, column);

            if (gameIsWon ())
            {
                showWin ();
                running = false;
            }
        }
    }

    private void rippleExpose (int row, int column)
    {
        int nAdjMines = countAdjacentMines (row, column);
        tagCell (row, column, (byte) (TAG_ZERO + nAdjMines));

        if (nAdjMines == 0)
        {
            int lowRow = row - 1;
            if (lowRow < 0) lowRow = 0;
            int hiRow = row + 2;
            if (hiRow > rows) hiRow = rows;

            int lowCol = column - 1;
            if (lowCol < 0) lowCol = 0;
            int hiCol = column + 2;
            if (hiCol > columns) hiCol = columns;

            for (int r = lowRow; r < hiRow; ++r)
            {
                for (int c = lowCol; c < hiCol; ++c)
                {
                    if (!cellIsExposed (r, c) && tags[r][c] != TAG_FLAG)
                    {
                        ++exposedCount;
                        rippleExpose (r, c);
                    }
                }
            }
        }
    }

    private int countAdjacentMines (int row, int column)
    {
        int count = 0;

        int lowRow = row - 1;
        if (lowRow < 0) lowRow = 0;
        int hiRow = row + 1;
        if (hiRow >= rows) hiRow = rows - 1;

        int lowCol = column - 1;
        if (lowCol < 0) lowCol = 0;
        int hiCol = column + 1;
        if (hiCol >= columns) hiCol = columns - 1;

        for (int r = lowRow; r <= hiRow; ++r)
        {
            for (int c = lowCol; c <= hiCol; ++c)
            {
                count += mined[r][c];
            }
        }

        count -= mined[row][column];

        return count;
    }

    private void showLoss ()
    {
        timerRunning = false;
        ticking = false;

        for (int row = rows; --row >= 0; )
        {
            for (int column = columns; --column >= 0; )
            {
                if (tags[row][column] == TAG_BOOM)
                    continue;

                boolean isMined = mined[row][column] != 0;
                boolean isFlagged = tags[row][column] == TAG_FLAG;

                if (isMined != isFlagged)
                {
                    tagCell (row, column, isMined ? TAG_MINE : TAG_OOPS);
                }
            }
        }
    }

    private void showWin ()
    {
        timerRunning = false;
        ticking = false;
        counter = 0;
        // update hi scores  NYI

        for (int row = tags.length; --row >= 0; )
        {
            for (int column = tags[row].length; --column >= 0; )
            {
                switch (tags[row][column])
                {
                case TAG_NULL:
                case TAG_QUES:
                    tagCell (row, column, TAG_FLAG);
                }
            }
        }
    }

    private void clearAround (int row, int column)
    {
        byte tag = tags[row][column];
        if (tag <= TAG_ZERO)
            return;

        int nAdjMines = tag - TAG_ZERO;

        int lowRow = row - 1;
        if (lowRow < 0) lowRow = 0;
        int hiRow = row + 1;
        if (hiRow >= rows) hiRow = rows - 1;

        int lowCol = column - 1;
        if (lowCol < 0) lowCol = 0;
        int hiCol = column + 1;
        if (hiCol >= columns) hiCol = columns - 1;

        int nAdjFlags = 0;
        for (int r = lowRow; r <= hiRow; ++r)
        {
            for (int c = lowCol; c <= hiCol; ++c)
            {
                if (tags[r][c] == TAG_FLAG)
                    ++nAdjFlags;
            }
        }

        if (nAdjFlags != nAdjMines)
            return;

        boolean lost = false;
        for (int r = lowRow; r <= hiRow; ++r)
        {
            for (int c = lowCol; c <= hiCol; ++c)
            {
                if (!cellIsExposed (r, c) && tags[r][c] != TAG_FLAG)
                {
                    if (mined[r][c] != 0)
                    {
                        tagCell (r, c, TAG_BOOM);
                        lost = true;
                    }
                    else
                    {
                        ++exposedCount;
                        rippleExpose (r, c);
                    }
                }
            }
        }

        if (lost)
        {
            showLoss ();
            running = false;
        }
        else if (gameIsWon ())
        {
            showWin ();
            running = false;
        }
    }

    //---------------------------------------------------------------------
    // Graphics layout.
    //---------------------------------------------------------------------

    private int computeInnerWidth (int columns)
    {
        return (columns * cellWidth) + (2 * gridBorderWidth);
    }

    private Rectangle computePanelRect (int rows, int columns)
    {
        Rectangle rect = computeGridPanelRect (rows, columns);
        rect.x = 0;
        rect.y = 0;
        rect.width += 2 * panelBorderWidth;
        rect.height += topPanelHeight + panelDividerHeight +
                                        (2 * panelBorderHeight);
        return rect;
    }

    private Rectangle computeTopPanelRect (int columns)
    {
        return new Rectangle (panelBorderWidth, panelBorderHeight,
                              computeInnerWidth (columns), topPanelHeight);
    }

    private Rectangle computeGridPanelRect (int rows, int columns)
    {
        Rectangle rect = computeGridRect (rows, columns);
        rect.x -= gridBorderWidth;
        rect.y -= gridBorderHeight;
        rect.width += 2 * gridBorderWidth;
        rect.height += 2 * gridBorderHeight;
        return rect;
    }

    private Rectangle computeGridRect (int rows, int columns)
    {
        int x = panelBorderWidth + gridBorderWidth;
        int y = topPanelHeight + panelBorderHeight + 
                        panelDividerHeight + gridBorderHeight;
        int width = columns * cellWidth;
        int height = rows * cellHeight;
        return new Rectangle (x, y, width, height);
    }
        
    private Rectangle computeRestartButtonRect (int columns)
    {
        int side = restartButtonSide;
        int x = panelBorderWidth + ((computeInnerWidth (columns) - side) / 2);
        int y = panelBorderHeight + ((topPanelHeight - side) / 2);
        return new Rectangle (x, y, side, side);
    }

    public Rectangle getCounterRect ()
    {
        int topBottomMargin = 6;
        int leftMargin = 8;
        int height = topPanelHeight - 2 * topBottomMargin;
        return new Rectangle (panelBorderWidth + leftMargin, 
                              panelBorderHeight + topBottomMargin,
                              countBoxWidth, height);
    }

    public Rectangle getTimerRect ()
    {
        int topBottomMargin = 6;
        int rightMargin = 8;
        int height = topPanelHeight - 2 * topBottomMargin;
        int x = panelBorderWidth + topPanelRect.width - 
                                rightMargin - countBoxWidth;
        return new Rectangle (x, panelBorderHeight + topBottomMargin,
                              countBoxWidth, height);
    }

    public Rectangle getCellRect (int row, int column)
    {
        int x = gridRect.x + column * cellWidth;
        int y = gridRect.y + row * cellHeight;
        return new Rectangle (x, y, cellWidth, cellHeight);
    }

    public Point scaleToRowColumn (int x, int y)
    {
        int relx = x - gridRect.x;
        int rely = y - gridRect.y;
        return new Point (relx / cellWidth, rely / cellHeight);
    }

    //---------------------------------------------------------------------
    // Images.
    //---------------------------------------------------------------------

    private static ColorModel makeColorModel ()
    {
        Color[] colors = {
            Color.black,                // the transparent color 
            Color.black,
            Color.white,
            Color.red,
            new Color (127, 0, 0),      // dark red
            Color.yellow,
            new Color (0, 127, 0),      // dark green
            Color.blue,
            new Color (0, 0, 127),      // dark blue
            new Color (127, 127, 0),    // brown
            new Color (0, 127, 127)     // dark cyan
        };

        byte[] reds = new byte [colors.length];
        byte[] greens = new byte [colors.length];
        byte[] blues = new byte [colors.length];

        for (int i = 0; i < colors.length; ++i)
        {
            reds[i] = (byte) colors[i].getRed ();
            greens[i] = (byte) colors[i].getGreen ();
            blues[i] = (byte) colors[i].getBlue ();
        }

        return new IndexColorModel (5, colors.length, reds, greens, blues, 0);
    }

    private void createImages ()
    {
        tagImages[TAG_FLAG] = createTag ("aaaaaaaaaaaaaaa" +
                                         "aaaaaaddaaaaaaa" +
                                         "aaaaddddaaaaaaa" +
                                         "aaadddddaaaaaaa" +
                                         "aaaaddddaaaaaaa" +
                                         "aaaaaaddaaaaaaa" +
                                         "aaaaaaabaaaaaaa" +
                                         "aaaaaaabaaaaaaa" +
                                         "aaaaabbbbaaaaaa" +
                                         "aaabbbbbbbbaaaa" +
                                         "aaabbbbbbbb");

        tagImages[TAG_QUES] = createTag ("aaaaaaaaaaaaaaa" +
                                         "aaaaabbbbaaaaaa" +
                                         "aaaabbaabbaaaaa" +
                                         "aaaabbaabbaaaaa" +
                                         "aaaaaaaabbaaaaa" +
                                         "aaaaaaabbaaaaaa" +
                                         "aaaaaabbaaaaaaa" +
                                         "aaaaaabbaaaaaaa" +
                                         "aaaaaaaaaaaaaaa" +
                                         "aaaaaabbaaaaaaa" +
                                         "aaaaaabb");

        tagImages[TAG_MINE] = 
        tagImages[TAG_BOOM] = createTag ("aaaaaaabaaaaaaa" +
                                         "aaaaaaabaaaaaaa" +
                                         "aaababbbbbabaaa" +
                                         "aaaabbbbbbbaaaa" +
                                         "aaabbccbbbbbaaa" +
                                         "aaabbccbbbbbaaa" +
                                         "abbbbbbbbbbbbba" +
                                         "aaabbbbbbbbbaaa" +
                                         "aaabbbbbbbbbaaa" +
                                         "aaaabbbbbbbaaaa" +
                                         "aaababbbbbabaaa" +
                                         "aaaaaaabaaaaaaa" +
                                         "aaaaaaab");

        tagImages[TAG_OOPS] = createTag ("aaaaaaabaaaaaaa" +
                                         "addaaaabaaaadda" +
                                         "aaddabbbbbaddaa" +
                                         "aaaddbbbbbddaaa" +
                                         "aaabddcbbddbaaa" +
                                         "aaabbddbddbbaaa" +
                                         "abbbbbdddbbbbba" +
                                         "aaabbbdddbbbaaa" +
                                         "aaabbddbddbbaaa" +
                                         "aaaaddbbbddaaaa" +
                                         "aaaddbbbbbddaaa" +
                                         "aaddaaabaaaddaa" +
                                         "addaaaabaaaadda" +
                                         "ddaaaaaaaaaaadd");

        tagImages[TAG_ONE] = createTag ("aaaaaaaaaaaaaaa" +
                                        "aaaaaaahhaaaaaa" +
                                        "aaaaaahhhaaaaaa" +
                                        "aaaaahhhhaaaaaa" +
                                        "aaaahhhhhaaaaaa" +
                                        "aaaaaahhhaaaaaa" +
                                        "aaaaaahhhaaaaaa" +
                                        "aaaaaahhhaaaaaa" +
                                        "aaaaaahhhaaaaaa" +
                                        "aaaahhhhhhhaaaa" +
                                        "aaaahhhhhhh");

        tagImages[TAG_TWO] = createTag ("aaaaaaaaaaaaaaa" +
                                        "aaaggggggggaaaa" +
                                        "aaggggggggggaaa" +
                                        "aagggaaaagggaaa" +
                                        "aaaaaaaaagggaaa" +
                                        "aaaaaaaggggaaaa" +
                                        "aaaaagggggaaaaa" +
                                        "aaagggggaaaaaaa" +
                                        "aaggggaaaaaaaaa" +
                                        "aaggggggggggaaa" +
                                        "aagggggggggg");

        tagImages[TAG_THREE] = createTag ("aaaaaaaaaaaaaaa" +
                                          "aadddddddddaaaa" +
                                          "aaddddddddddaaa" +
                                          "aaaaaaaaadddaaa" +
                                          "aaaaaaaaadddaaa" +
                                          "aaaaaddddddaaaa" +
                                          "aaaaaddddddaaaa" +
                                          "aaaaaaaaadddaaa" +
                                          "aaaaaaaaadddaaa" +
                                          "aaddddddddddaaa" +
                                          "aaddddddddd");

        tagImages[TAG_FOUR] = createTag ("aaaaaaaaaaaaaaa" +
                                         "aaaaiiiaiiiaaaa" +
                                         "aaaaiiiaiiiaaaa" +
                                         "aaaiiiaaiiiaaaa" +
                                         "aaaiiiaaiiiaaaa" +
                                         "aaiiiiiiiiiiaaa" +
                                         "aaiiiiiiiiiiaaa" +
                                         "aaaaaaaaiiiaaaa" +
                                         "aaaaaaaaiiiaaaa" +
                                         "aaaaaaaaiiiaaaa" +
                                         "aaaaaaaaiii");

        tagImages[TAG_FIVE] = createTag ("aaaaaaaaaaaaaaa" +
                                         "aaeeeeeeeeeeaaa" +
                                         "aaeeeeeeeeeeaaa" +
                                         "aaeeeaaaaaaaaaa" +
                                         "aaeeeaaaaaaaaaa" +
                                         "aaeeeeeeeeeaaaa" +
                                         "aaeeeeeeeeeeaaa" +
                                         "aaaaaaaaaeeeaaa" +
                                         "aaaaaaaaaeeeaaa" +
                                         "aaeeeeeeeeeeaaa" +
                                         "aaeeeeeeeee");

        tagImages[TAG_SIX] = createTag ("aaaaaaaaaaaaaaa" +
                                        "aaakkkkkkkkaaaa" +
                                        "aakkkkkkkkkaaaa" +
                                        "aakkkaaaaaaaaaa" +
                                        "aakkkaaaaaaaaaa" +
                                        "aakkkkkkkkkaaaa" +
                                        "aakkkkkkkkkkaaa" +
                                        "aakkkaaaakkkaaa" +
                                        "aakkkaaaakkkaaa" +
                                        "aakkkkkkkkkkaaa" +
                                        "aaakkkkkkkk");

        tagImages[TAG_SEVEN] = createTag ("aaaaaaaaaaaaaaa" + 
                                          "aabbbbbbbbbbaaa" + 
                                          "aabbbbbbbbbbaaa" + 
                                          "aaaaaaaaabbbaaa" + 
                                          "aaaaaaaaabbbaaa" + 
                                          "aaaaaaaabbbaaaa" + 
                                          "aaaaaaaabbbaaaa" + 
                                          "aaaaaaabbbaaaaa" + 
                                          "aaaaaaabbbaaaaa" + 
                                          "aaaaaabbbaaaaaa" + 
                                          "aaaaaabbb");

        tagImages[TAG_EIGHT] = createTag ("aaaaaaaaaaaaaaa" +
                                          "aaajjjjjjjjaaaa" +
                                          "aajjjjjjjjjjaaa" +
                                          "aajjjaaaajjjaaa" +
                                          "aajjjaaaajjjaaa" +
                                          "aaajjjjjjjjaaaa" +
                                          "aaajjjjjjjjaaaa" +
                                          "aajjjaaaajjjaaa" +
                                          "aajjjaaaajjjaaa" +
                                          "aajjjjjjjjjjaaa" +
                                          "aaajjjjjjjj");

        happyImage = createIcon ("aaaaaabbbbbaaaaaa" +
                                 "aaaabbfffffbbaaaa" +
                                 "aaabfffffffffbaaa" +
                                 "aabafffffffffabaa" +
                                 "abafffffffffffaba" +
                                 "abaffbbfffbbffaba" +
                                 "bffffbbfffbbffffb" +
                                 "bfffffffffffffffb" +
                                 "bfffffffffffffffb" +
                                 "bfffffffffffffffb" +
                                 "bfffbfffffffbfffb" +
                                 "abaffbfffffbffaba" +
                                 "abafffbbbbbfffaba" +
                                 "aabafffffffffabaa" +
                                 "aaabfffffffffbaaa" +
                                 "aaaabbfffffbbaaaa" +
                                 "aaaaaabbbbbaaaaaa",
                                 17);

        scaredImage = createIcon ("aaaaaabbbbbaaaaaa" +
                                  "aaaabbfffffbbaaaa" +
                                  "aaabfffffffffbaaa" +
                                  "aabafffffffffabaa" +
                                  "abafjbjfffjbjfaba" +
                                  "abafbbbfffbbbfaba" +
                                  "bfffjbjfffjbjfffb" +
                                  "bfffffffffffffffb" +
                                  "bfffffffffffffffb" +
                                  "bffffffbbbffffffb" +
                                  "bfffffbbfbbfffffb" +
                                  "abafffbfffbfffaba" +
                                  "abafffbbfbbfffaba" +
                                  "aabafffbbbfffabaa" +
                                  "aaabfffffffffbaaa" +
                                  "aaaabbfffffbbaaaa" +
                                  "aaaaaabbbbbaaaaaa",
                                  17);

        deadImage = createIcon ("aaaaaabbbbbaaaaaa" +
                                "aaaabbfffffbbaaaa" +
                                "aaabfffffffffbaaa" +
                                "aabafffffffffabaa" +
                                "abafbfbfffbfbfaba" +
                                "abaffbfffffbffaba" +
                                "bfffbfbfffbfbfffb" +
                                "bfffffffffffffffb" +
                                "bfffffffffffffffb" +
                                "bfffffffffffffffb" +
                                "bfffffbbbbbfffffb" +
                                "abaffbfffffbffaba" +
                                "abafbfffffffbfaba" +
                                "aabafffffffffabaa" +
                                "aaabfffffffffbaaa" +
                                "aaaabbfffffbbaaaa" +
                                "aaaaaabbbbbaaaaaa",
                                17);

        coolImage = createIcon ("aaaaaabbbbbaaaaaa" +
                                "aaaabbfffffbbaaaa" +
                                "aaabfffffffffbaaa" +
                                "aabafffffffffabaa" +
                                "abafffffffffffaba" +
                                "abafbbbbbbbbbfaba" +
                                "bffbbbbbfbbbbbffb" +
                                "bfbfbbbbfbbbbfbfb" +
                                "bbffjbbfffbbjffbb" +
                                "bfffffffffffffffb" +
                                "bfffffffffffffffb" +
                                "abaffbfffffbffaba" +
                                "abafffbbbbbfffaba" +
                                "aabafffffffffabaa" +
                                "aaabfffffffffbaaa" +
                                "aaaabbfffffbbaaaa" +
                                "aaaaaabbbbbaaaaaa",
                                17);

        //
        // An array matching each pixel of the seven segment display to 
        // the segment it belongs to:
        //
        //      1
        //     2 3
        //      4
        //     5 6
        //      7
        //
        String sevenSegTemplate =
            "01111111110" +
            "20111111103" +
            "22011111033" +
            "22200000333" +
            "22200000333" +
            "22200000333" +
            "22200000333" +
            "22200000333" +
            "22000000033" +
            "20444444403" +
            "04444444440" +
            "50444444406" +
            "55000000066" +
            "55500000666" +
            "55500000666" +
            "55500000666" +
            "55500000666" +
            "55500000666" +
            "55077777066" +
            "50777777706" +
            "07777777770"
        ;

        for (int digit = 0; digit < sevenSegImages.length; ++digit)
        {
            // Map each digit to its set of "lit" segments...
            int pattern = 0;
            switch (digit)
            {
            case 0:
                pattern = 0x77;
                break;
            case 1:
                pattern = 0x12;
                break;
            case 2:
                pattern = 0x5d;
                break;
            case 3:
                pattern = 0x5b;
                break;
            case 4:
                pattern = 0x3a;
                break;
            case 5:
                pattern = 0x6b;
                break;
            case 6:
                pattern = 0x6f;
                break;
            case 7:
                pattern = 0x52;
                break;
            case 8:
                pattern = 0x7f;
                break;
            case 9:
                pattern = 0x7b;
                break;
            case 10:
                pattern = 0x08; // the minus sign
            }

            byte[] data = new byte [sevenSegTemplate.length ()];
            for (int i = 0; i < data.length; ++i)
            {
                byte pixel = 1; // default is black.
                int seg = sevenSegTemplate.charAt (i) - '0';
                if (seg != 0)
                {
                    int mask = 1 << (7 - seg);
                    if ((pattern & mask) != 0)
                        pixel = 3;      // light red
                    else if (i % 2 == 0)
                        pixel = 4;      // dark red
                }
                data[i] = pixel;
            }

            sevenSegImages[digit] = createImage (data, 11, 21);
        }
    }

    private Image createTag (String stringData)
    {
        return createIcon (stringData, 15);
    }

    private Image createIcon (String stringData, int width)
    {
        int dataLength = stringData.length ();
        int height = (dataLength / width) + 1;
        byte[] data = new byte [width * height];

        for (int i = 0; i < dataLength; ++i)
        {
            data[i] = (byte) (stringData.charAt (i) - 'a');
        }

        return createImage (data, width, height);
    }

    private Image createImage (byte[] data, int width, int height)
    {
        return createImage (new MemoryImageSource (width, height, 
                                                   colorModel,
                                                   data, 0, width));
    }

    //---------------------------------------------------------------------
    // Graphics.
    //---------------------------------------------------------------------

    /**
     * An extension to Graphics.draw3DRect.  Features variable thickness
     * border.
     */
    private static void draw3DRect (Graphics g, Rectangle rect,
                                   boolean raised, int thickness)
    {
        draw3DRect (g, rect.x, rect.y, rect.width, rect.height, 
                    raised, thickness);
    }

    /**
     * An extension to Graphics.draw3DRect.  Features variable thickness
     * border.
     */
    private static void draw3DRect (Graphics g, int x, int y, 
                                   int width, int height, 
                                   boolean raised, int thickness)
    {
        Color color = g.getColor();
        Color brighter = color.brighter();
        Color darker = color.darker();

        int left = x;
        int right = x + width - 1;
        int top = y;
        int bottom = y + height - 1;
                
        while (--thickness >= 0)
        {
            // Draw top, left sides.
            //
            g.setColor (raised ? brighter : darker);
            g.drawLine (left, top, right, top);
            g.drawLine (left, top, left, bottom);

            // Draw bottom, right sides.
            g.setColor (raised ? darker : brighter);
            g.drawLine (left, bottom, right, bottom);
            g.drawLine (right, top, right, bottom);

            ++left;
            ++top;
            --right;
            --bottom;
        }

        g.setColor (color);
    }

    public void paintRestartButton (Graphics g)
    {
        int x = restartButtonRect.x;
        int y = restartButtonRect.y;
        int width = restartButtonRect.width;
        int height = restartButtonRect.height;

        g.fillRect (x, y, width - 1, height - 1);

        if (restartPressed)
        {
            g.setColor (darkerColor);
            g.drawLine (x + 1, y + 1, x + width - 1, y + 1);
            g.drawLine (x + 1, y + 1, x + 1, y + height - 1);
        }
        else
        {
            // Draw inner 3D effect
            draw3DRect (g, x + 1, y + 1, width - 2, height - 2, true, 2);
        }

        // Put a dark border on it.
        g.setColor (darkerColor);
        g.drawLine (x, y, x + width - 2, y);
        g.drawLine (x, y, x, y + height - 2);
        g.drawLine (x + width - 1, y + 1, x + width - 1, y + height - 1);
        g.drawLine (x + 1, y + height - 1, x + width - 1, y + height - 1);

        g.setColor (Color.lightGray);
        paintRestartImage (g);
    }

    private void paintRestartImage (Graphics g)
    {
        int x = restartButtonRect.x + 5;
        int y = restartButtonRect.y + 5;
        if (restartPressed)
        {
            x += 1;
            y += 1;
        }
        g.drawImage (restartImage, x, y, null);
    }

    private void paintCell (Graphics g, int row, int column, boolean pressed)
    {
        boolean covered = !pressed && !cellIsExposed (row, column);
        int x = gridRect.x + column * cellWidth;
        int y = gridRect.y + row * cellHeight;
        int width = cellWidth;
        int height = cellHeight;
        int tag = tags[row][column];

        if (!covered)
        {
            g.setColor (darkerColor);
            g.drawLine (x, y, x + width - 1, y);
            g.drawLine (x, y, x, y + height - 1);
            g.setColor (tag == TAG_BOOM ? Color.red : Color.lightGray);
            g.fillRect (x + 1, y + 1, width - 1, height - 1);
        }
        else
        {
            draw3DRect (g, x, y, width, height, true, 2);
            g.fillRect (x + 2, y + 2, width - 4, height - 4);
        }

        Image tagImage = tagImages[tags[row][column]];
        if (tagImage != null)
        {
            g.drawImage (tagImage, x + 1, y + 2, null);
        }
    }

    public void drawSevenSeg (Graphics g, Rectangle rect, int[] digits,
                              int value, boolean fullPaint)
    {
        if (fullPaint)
        {
            draw3DRect (g, rect, false, 1);
            Color c = g.getColor ();
            g.setColor (Color.black);
            g.fillRect (rect.x + 1, rect.y + 1, rect.width - 2,
                        rect.height - 2);
            g.setColor (c);
        }

        int digitWidth = (rect.width - 2 - digits.length) / 3 + 1;

        int v = Math.abs (value);
        for (int i = digits.length; --i >= 0; )
        {
            int digVal = (i == 0 && value < 0) ? 10 : (v % 10);
            v /= 10;
            if (fullPaint || digVal != digits[i])
            {
                g.drawImage (sevenSegImages[digVal], 
                             rect.x + 2 + (i * digitWidth),
                             rect.y + 2, null);
            }
            digits[i] = digVal;
        }
    }
}
