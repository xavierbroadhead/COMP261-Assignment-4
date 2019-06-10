# COMP261 Assignment 4
A fully functional parser/scripting language for a simple game in java. The game itself runs on scripted commands. The robots constantly lose fuel, and when they reach 0 fuel left they die. Barrels are randomly spawned on the grid and these replenish the robots' fuel, however only one robot can be on any given tile at any given time. An example file has been provided to demonstrate syntax.

# Syntax

| Keywords | Function | Example syntax |
| -------- | -------- | -------------- |
| loop| Loops indefinitely| loop{...} |
| while(cond) | Loops the block while the condition is true | while(not(eq(add(5, fuelLeft), 75))){...}|
| if(cond)    | Executes the block if the condition is true | if(gt(50, fuelLeft){...}|
| elif(cond)  | If the above if statement's condition isn't true, execute this block | elif(gt(25, fuelLeft){...}|
| else        | If neither the if nor the elif statement(s) are true, execute this block | else{...}|
| move(args)/wait(args)/turnL/turnR/  takeFuel/turnAround/shieldOn/shieldOff | Executes the command | wait(8);, turnL;|
| lt(arg, arg)/gt(arg, arg)/eq(arg, arg) | Returns true if the first argument is less than, greater than, or equal to the second argument respectively | if(lt(5, 6)){...}, elif(lt(fuelLeft, 100)){...}|
|fuelLeft/numBarrels/wallDist | Returns current fuel left, number of fuel barrels available, and current distance to closest wall, respectively | add(5, fuelLeft);|
| oppLR/oppFB/barrelLR/barrelFB | Returns the left-right (LR), or front-back (FB) location of the opponent or closest barrel, respectively | while(lt(5, barrelLR)){...}|
| and(cond, cond)/not(cond)/or(cond, cond) | Returns true if the condition is evaluated to be true. Can only be used with an operator | if(and(lt(5, 6), eq(60, fuelLeft))){...}|


# The game interface
![Game UI](https://github.com/xavierbroadhead/COMP261-Assignment-4/blob/master/Robot%20game%20example.png?raw=true)
