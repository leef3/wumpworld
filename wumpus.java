import java.io.*;
import java.util.Date;
import java.util.*;
import java.text.*;
import java.util.Scanner;

//Block Object - Holds Percepts Generated at Read-in from input.txt
//Block Object also holds hard values of WUMPUS and PIT to track Game Over
class Block
{
	boolean BREEZE, STENCH, GLITTER;
	boolean WUMPUS, PIT;

    //For player/knowledge base use only
    boolean SAFE;
	public Block()
	{
		//Percepts
		BREEZE = false;
		STENCH = false;
		GLITTER = false;

		//Hard Values
		WUMPUS = false;
		PIT = false;

		//Used for player's board (What is observed along the way)
		SAFE = false;
        //Safe counts for visited too
	}
}
class wumpus
{
	/*
		NOTE: BOARD IS SETUP IN THE PROGRAM AS
			 0  1  2  3 
		  0 [ ][ ][ ][ ]
		  1 [ ][ ][ ][ ]
		  2 [ ][ ][ ][ ]
		  3 [ ][ ][ ][ ]

		BUT METHOD CALLED "boardPosTranslate()" CHANGES IT TO DISPLAY CORRECTLY

			 1  2  3  4 
		  4 [ ][ ][ ][ ]
		  3 [ ][ ][ ][ ]
		  2 [ ][ ][ ][ ]
		  1 [ ][ ][ ][ ]

	*/
	//Game Board with all percepts
	private static Block[][] masterBoard;

	//Global Variables to track player position and direction
	//EAST = 90 | SOUTH = 180 | WEST = 270 | NORTH = 0
	//Turns achieved with (facingDirection + 90) % 360
	private static int facingDirection;
	//Position of Player
	private static int playerX, playerY;

	//Wumpus Position
	private static int wumpusX, wumpusY;
	//Single Arrow In Inventory
	private static boolean arrowAvailable;

	//Player's observable board (Part of the Knowledge base) Keeps track of percepts
	private static Block[][] playerBoard;

	public static void main(String args[]) throws Exception
    {
    	FileReader fr = new FileReader("input.txt");
    	BufferedReader inputReader = new BufferedReader(fr);

    	//For N x N Input Checking
    	int rowCount = 0;
    	int colCount = 0;

    	String nextLine = inputReader.readLine();
    	List<String> setBoardSize = Arrays.asList(nextLine.split(","));
    	//Assumes board input is correct sets up 2D array, will scrap later if board input invalid
    	masterBoard = new Block[setBoardSize.size()][setBoardSize.size()];
        playerBoard = new Block[setBoardSize.size()][setBoardSize.size()];
    	//Create the Blocks in the Board (As a placeholder)
    	for(int p = 0; p < setBoardSize.size(); p++)
    	{
    		for(int j = 0; j < setBoardSize.size(); j++)
    		{
    			masterBoard[p][j] = new Block();
    			playerBoard[p][j] = new Block();
    		}
    	}
    	while(nextLine != null)
    	{
    		//Split the first line
    		List<String> inputParse = Arrays.asList(nextLine.split(","));
    		//Add it to the board including percepts
    		setupBoard(rowCount, inputParse);

    		colCount = inputParse.size();
    		rowCount++;
    		//System.out.println(nextLine);
    		nextLine = inputReader.readLine();
    	}

    	//Checking for a valid initial input DOES NOT ACCOUNT FOR VARYING ROW SIZE (Was not told this was a requirement)
    	//Does check for N X N board
    	if(rowCount != colCount)
    	{
    		//Not an N x N board reject and start over
    		System.out.println("Not N x N Input! Check your input.txt and try again!");
    		return;
    	}
    	else
    	{
    		//Continue
    		System.out.println("Board Input: " + rowCount + " x " + colCount + " ...Valid ... Proceeding...");
            clearKnowledgeBase();
    	}

    	//============================BEGIN GAME / USER INPUT========================
    	System.out.println("Welcome Adventurer!... To Wumpus World");
    	System.out.println("Enter 'R' to turn Right 90 Degrees, 'L' to turn Left 0 90 Degrees, 'F' to move Forward, and 'S' to shoot and arrow");
    	
    	//Set up initial player position [3][0] translates to [1][1]
    	playerX = 0;
    	playerY = masterBoard[0].length - 1;
    	facingDirection = 90;
    	arrowAvailable = true;
        playerBoard[0][masterBoard[0].length - 1].SAFE = true;

    	BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
    	while(true)
    	{
    		//Prints Position and Facing Direction and current Block's Percepts
    		printStatus();
    		//System.out.println(playerX + "|" + playerY);
    		//Take console input
    		String charEntry = console.readLine();
    		if(charEntry.equals("R"))
    		{
    			facingDirection = (facingDirection + 90) % 360;
    		}
    		else if(charEntry.equals("L"))
    		{
    			facingDirection = (facingDirection + 270) % 360;
    		}
    		else if(charEntry.equals("F"))
    		{
    			moveForward();
                if(playerBoard[playerX][playerY].SAFE == false)
                {
                    updateKnowledgeBase("SAFE", playerX, playerY);
                    playerBoard[playerX][playerY].SAFE = true;
                }
    			//Check if dead or gold found
    			if(isKilled()){return;}
    			if(isWon()){return;}
    		}
    		else if(charEntry.equals("S"))
    		{
    			if(arrowAvailable)
    				{
    					fireArrow();
    					arrowAvailable = false;
    				}
    			else
    				{System.out.println("ARROW UNAVAILABLE");}
    		}
    		else
    		{
    			//Invalid input
    			System.out.println("Invalid Char Entry... Only R,L,F,S Allowed");
    		}
    	}

    	//=================================END USER INPUT======================
    }

    //============================BEGIN BOARD SETUP METHODS=========================

    private static void setupBoard(int rowNum, List<String> parsedInput)
    {
    	for(int x = 0; x < parsedInput.size(); x++)
    	{
    		//Do nothing if the input is X

    		//Add surrounding percepts for W (Wumpus)
    		if(parsedInput.get(x).equals("W"))
    		{
    			masterBoard[x][rowNum].WUMPUS = true;
    			addSurroundingPercept("W", x, rowNum);
    			//Record in global variable
    			wumpusX = x;
    			wumpusY = rowNum;
    		}
    		//Add surrounding percepts for P (Pit)
    		if(parsedInput.get(x).equals("P"))
    		{
    			masterBoard[x][rowNum].PIT = true;
    			addSurroundingPercept("P", x, rowNum);
    		}
    		//Directly add GLITTER for G (Gold) because no surrounding percepts
    		if(parsedInput.get(x).equals("G")){masterBoard[x][rowNum].GLITTER = true;}
    	}
    }

    private static void addSurroundingPercept(String entry, int posX, int posY)
    {
		//Check Upper Boundary
		if((posY - 1) >= 0)
		{
			if(entry.equals("W")){masterBoard[posX][posY-1].STENCH = true;}
			if(entry.equals("P")){masterBoard[posX][posY-1].BREEZE = true;}
		}
		//Check Lower Boundary
		if((posY + 1) < masterBoard[0].length)
		{
			if(entry.equals("W")){masterBoard[posX][posY+1].STENCH = true;}
			if(entry.equals("P")){masterBoard[posX][posY+1].BREEZE = true;}
		}
		//Check Left Boundary
		if((posX - 1) >=0)
		{
			if(entry.equals("W")){masterBoard[posX-1][posY].STENCH = true;}
			if(entry.equals("P")){masterBoard[posX-1][posY].BREEZE = true;}
		}
		//Check Right Boundary
		if((posX + 1) < masterBoard[0].length)
		{
			if(entry.equals("W")){masterBoard[posX+1][posY].STENCH = true;}
			if(entry.equals("P")){masterBoard[posX+1][posY].BREEZE = true;}
		}
    }
    //============================================END SETUP METHODS==========================


    //===================================BEGIN UPDATE/PLAY METHODS======================
    private static void printStatus()
    {
    	//System.out.println("DEBUG ME: " + playerX + " " + playerY);
    	boardPosTranslate(playerX, playerY);
    	printDirection();
    	printPercepts();

        hintKnowledgeBase();
    }
    private static void printDirection()
    {
    	if(facingDirection == 90){System.out.println("Facing EAST");}
    	if(facingDirection == 180){System.out.println("Facing SOUTH");}
    	if(facingDirection == 270){System.out.println("Facing WEST");}
    	if(facingDirection == 0){System.out.println("Facing NORTH");}
    }
    private static void boardPosTranslate(int originalX, int originalY)
    {
    	int displayOutY = Math.abs(originalY - masterBoard[0].length);
    	int displayOutX = originalX + 1;
    	System.out.println("You are in room [" + displayOutX + "," + displayOutY + "] of the cave");
    }
    private static void printPercepts()
    {
    	Block toCheck = masterBoard[playerX][playerY];
    	if(toCheck.BREEZE == true)
    	{
    		//Add to KNOWLEDGE BASE
    		System.out.println("You feel a BREEZE");
            if(playerBoard[playerX][playerY].BREEZE == false)
            {
                updateKnowledgeBase("BREEZE", playerX, playerY);
                playerBoard[playerX][playerY].BREEZE = true;
            } 
    	}
    	if(toCheck.STENCH == true)
    	{
    		//Add to KNOWLEDGE BASE
    		System.out.println("You smell a STENCH");
            if(playerBoard[playerX][playerY].STENCH == false)
            {
                updateKnowledgeBase("STENCH", playerX, playerY);
                playerBoard[playerX][playerY].STENCH = true;
            } 
    	}
    }
    //Shoot Arrow Function -- Also Checks if Wumpus is dead and removes it if so
    //NOTE: This method only runs once as per instructions
    private static void fireArrow()
    {
    	System.out.println("Wumpus: " + wumpusX + " " + wumpusY);
    	//Fire EAST
    	if(facingDirection == 90)
    	{
    		if((wumpusY == playerY) && (wumpusX > playerX))
    		{
    			System.out.println("The Wumpus Screams in Agony! Direct Hit!");
    			//Remove the Wumpus
    			Block removeWumpus = masterBoard[wumpusX][wumpusY];
    			removeWumpus.WUMPUS = false;
    			return;
    		}
    	}
    	//Fire SOUTH
    	if(facingDirection == 180)
    	{
    		if((wumpusX == playerX) && (wumpusY > playerY))
    		{
    			System.out.println("The Wumpus Screams in Agony! Direct Hit!");
    			//Remove the Wumpus
    			Block removeWumpus = masterBoard[wumpusX][wumpusY];
    			removeWumpus.WUMPUS = false;
    			return;
    		}
    	}
    	//Fire WEST
    	if(facingDirection == 270)
    	{
    		if((wumpusY == playerY) && (wumpusX < playerX))
    		{
    			System.out.println("The Wumpus Screams in Agony! Direct Hit!");
    			//Remove the Wumpus
    			Block removeWumpus = masterBoard[wumpusX][wumpusY];
    			removeWumpus.WUMPUS = false;
    			return;
    		}
    	}
    	//Fire NORTH
    	if(facingDirection == 0)
    	{
    		if((wumpusX == playerX) && (wumpusY < playerY))
    		{
    			System.out.println("The Wumpus Screams in Agony! Direct Hit!");
    			//Remove the Wumpus
    			Block removeWumpus = masterBoard[wumpusX][wumpusY];
    			removeWumpus.WUMPUS = false;
    			return;
    		}
    	}
    	System.out.println("Your Arrow Hits a Wall and Misses, The Wumpus Chuckles...");
    	return;
    }
    //Check if dead by checking WUMPUS or PIT fact in current Block
    private static boolean isKilled()
    {
    	Block toCheck = masterBoard[playerX][playerY];
    	if(toCheck.PIT == true)
    	{
    		System.out.println("GAME OVER! - Killed by falling into a PIT");
    		return true;
    	}
    	if(toCheck.WUMPUS == true)
    	{
    		System.out.println("GAME OVER! - Killed by the WUMPUS");
    		return true;
    	}
    	return false;
    }
    //Check if gold is found by checking GLITTER percept (only in Block w/ gold)
    private static boolean isWon()
    {
    	Block toCheck = masterBoard[playerX][playerY];
    	if(toCheck.GLITTER == true)
    	{
    		System.out.println("YOU WIN!!! - GOLD FOUND!!");
    		return true;
    	}
    	return false;
    }
    private static boolean moveForward()
    {
    	//Facing East [2,2] --> [3,2]
    	if(facingDirection == 90)
    	{
    		if(((playerX + 1) < masterBoard[0].length) && ((playerX + 1) >= 0))
    		{
    			playerX = playerX + 1;
                return true;
    		}
    		else{System.out.println("BUMPED INTO A WALL!");}
    	}
    	//Facing South [2,2] --> [2,1]
    	if(facingDirection == 180)
    	{
    		if(((playerY + 1) < masterBoard[0].length) && ((playerY + 1) >= 0))
    		{
				playerY = playerY + 1;
                return true;
    		}
    		else{System.out.println("BUMPED INTO A WALL!");}
    	}
    	//Facing West [2,2] --> [1,2]
    	if(facingDirection == 270)
    	{
    		if(((playerX - 1) < masterBoard[0].length) && ((playerX - 1) >= 0))
    		{
    			playerX = playerX - 1;
                return true;
    		}
    		else{System.out.println("BUMPED INTO A WALL!");}
    	}
    	//Facing North [2,2] --> [2,3]
    	if(facingDirection == 0)
    	{
    		if(((playerY - 1) < masterBoard[0].length) && ((playerY - 1) >= 0))
    		{
    			playerY = playerY - 1;
                return true;
    		}
    		else{System.out.println("BUMPED INTO A WALL!");}
    	}
        return false;
    }
    //======================================END UPDATE/PLAY METHODS======================

    //=====================================KNOWLEDGE BASE METHODS========================
    private static void updateKnowledgeBase(String percept, int posX, int posY)
    {
        int displayOutY = Math.abs(posY - masterBoard[0].length);
        int displayOutX = posX + 1;
        try
        {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("knowledgebase.txt", true)));
            out.println("[" + displayOutX + "," + displayOutY + "] = " + percept);
            out.close();
        }
        catch (Exception e)
        {
            System.out.println("Output File Not Found!");
        }
    }
    private static void clearKnowledgeBase()
    {
        try
        {
            PrintWriter out = new PrintWriter("knowledgebase.txt");
            out.close();
        }
        catch (Exception e)
        {
            System.out.println("Output File Not Found!");
        }
    }
    private static void hintKnowledgeBase()
    {
        boolean northSafe, southSafe, eastSafe, westSafe;
        northSafe = southSafe = eastSafe = westSafe = true;
        //Activates when current  block has a BREEZE or STENCH percept
        if(playerBoard[playerX][playerY].BREEZE || playerBoard[playerX][playerY].STENCH)
        {
            //EAST BLOCK
            if(((playerX + 1) < masterBoard[0].length) && ((playerX + 1) >= 0))
            {
                if(!playerBoard[playerX+1][playerY].SAFE)
                {
                    //EAST IS UNSAFE AND UNKNOWN
                    System.out.println("Hint: Possible Danger In [" + (playerX+1) + "," + (Math.abs(playerY - masterBoard[0].length)) + "]");
                    updateKnowledgeBase("POSSIBLE DANGER", playerX+1, playerY);
                    eastSafe = false;
                }
                //CHECK NORTH BLOCK
                if(((playerY - 1) < masterBoard[0].length) && ((playerY - 1) >= 0))
                {
                    if(!playerBoard[playerX][playerY - 1].SAFE)
                    {
                        //NORTH IS UNSAFE AND UNKNOWN
                        System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                        updateKnowledgeBase("POSSIBLE DANGER", playerX, playerY - 1);
                        northSafe = false;
                    }

                    //NORTHEAST BLOCK
                    if(playerBoard[playerX + 1][playerY-1].BREEZE || playerBoard[playerX + 1][playerY-1].STENCH)
                    {
                        //If east is safe then north must be a pit
                        if(eastSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX, playerY-1);
                        }
                        //If north is safe then east must be a pit
                        else if(northSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX + 1) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX+1, playerY);

                        }
                    }
                }
                //CHECK SOUTH BLOCK
                if(((playerY + 1) < masterBoard[0].length) && ((playerY + 1) >= 0))
                {
                    if(!playerBoard[playerX][playerY + 1].SAFE)
                    {
                        //SOUTH IS UNSAFE AND UNKNOWN
                        System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length + 1)) + "]");
                        updateKnowledgeBase("POSSIBLE DANGER", playerX, playerY + 1);
                        southSafe = false;
                    }

                    //SOUTHEAST BLOCK
                    if(playerBoard[playerX + 1][playerY+1].BREEZE || playerBoard[playerX + 1][playerY+1].STENCH)
                    {
                        //If east is safe then south must be a pit
                        if(eastSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length + 1)) + "]");
                            updateKnowledgeBase("PIT", playerX, playerY+1);
                        }
                        //If south is safe then east must be a pit
                        else if(southSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX + 1) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX+1, playerY);

                        }
                    }
                }
            }


            //WEST BLOCK
            if(((playerX - 1) < masterBoard[0].length) && ((playerX - 1) >= 0))
            {
                if(!playerBoard[playerX-1][playerY].SAFE)
                {
                    //WEST IS UNSAFE AND UNKNOWN
                    System.out.println("Hint: Possible Danger In [" + (playerX-1) + "," + (Math.abs(playerY - masterBoard[0].length)) + "]");
                    updateKnowledgeBase("POSSIBLE DANGER", playerX-1, playerY);
                    westSafe = false;
                }
                //CHECK NORTH BLOCK
                if(((playerY - 1) < masterBoard[0].length) && ((playerY - 1) >= 0))
                {
                    if(!playerBoard[playerX][playerY - 1].SAFE)
                    {
                        //NORTH IS UNSAFE AND UNKNOWN
                        System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                        updateKnowledgeBase("POSSIBLE DANGER", playerX, playerY - 1);
                        northSafe = false;
                    }

                    //NORTHWEST BLOCK
                    if(playerBoard[playerX - 1][playerY-1].BREEZE || playerBoard[playerX - 1][playerY-1].STENCH)
                    {
                        //If west is safe then north must be a pit
                        if(westSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX, playerY-1);
                        }
                        //If north is safe then west must be a pit
                        else if(northSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX + 1) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX-1, playerY);

                        }
                    }
                }
                //CHECK SOUTH BLOCK
                if(((playerY + 1) < masterBoard[0].length) && ((playerY + 1) >= 0))
                {
                    if(!playerBoard[playerX][playerY + 1].SAFE)
                    {
                        //SOUTH IS UNSAFE AND UNKNOWN
                        System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length + 1)) + "]");
                        updateKnowledgeBase("POSSIBLE DANGER", playerX, playerY + 1);
                        southSafe = false;
                    }

                    //SOUTHWEST BLOCK
                    if(playerBoard[playerX - 1][playerY+1].BREEZE || playerBoard[playerX - 1][playerY+1].STENCH)
                    {
                        //If west is safe then south must be a pit
                        if(westSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX) + "," + (Math.abs(playerY - masterBoard[0].length + 1)) + "]");
                            updateKnowledgeBase("PIT", playerX, playerY+1);
                        }
                        //If south is safe then west must be a pit
                        else if(southSafe)
                        {
                            System.out.println("Hint: Possible Danger In [" + (playerX - 1) + "," + (Math.abs(playerY - masterBoard[0].length - 1)) + "]");
                            updateKnowledgeBase("PIT", playerX-1, playerY);

                        }
                    }
                }
            }
        }
    }
}
