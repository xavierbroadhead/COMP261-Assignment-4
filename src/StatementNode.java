import java.util.ArrayList;
import java.util.List;

public class StatementNode implements RobotProgramNode{

    loopNode loopNode;
    actionNode actionNode;
    whileNode whileNode;
    ifNode ifNode;

    public StatementNode(loopNode loopNode, actionNode actionNode, whileNode whileNode, ifNode ifNode){
        this.loopNode = loopNode;
        this.actionNode = actionNode;
        this.whileNode = whileNode;
        this.ifNode = ifNode;
    }

    @Override
    public void execute(Robot robot) {
        if (this.loopNode != null){
            this.loopNode.execute(robot);
        }
        else if (this.actionNode != null){
            this.actionNode.execute(robot);
        }
        else if (this.whileNode != null){
            this.whileNode.execute(robot);
        }
        else if (this.ifNode != null){
            this.ifNode.execute(robot);
        }
    }

    /**
     * ============= PROGRAM NODE ===============
     */
    static class programNode implements RobotProgramNode{

        List<StatementNode> program;

        public programNode(List<StatementNode> list){
            this.program = list;
        }

        @Override
        public void execute(Robot robot) {
            for (StatementNode statement : program){
                statement.execute(robot);
            }
        }
    }

    /**
     *  =================== CONDITIONAL NODES ===================
     */
    static class loopNode implements RobotProgramNode{

        blockNode block;

        public loopNode(blockNode b){
            this.block = b;
        }

        @Override
        public void execute(Robot robot) {
            while (!robot.isDead()) block.execute(robot);
        }
    }
    static class blockNode implements RobotProgramNode{

        List<StatementNode> statements;

        public blockNode(List<StatementNode> statements){
            this.statements = statements;
        }

        @Override
        public void execute(Robot robot) {
                for (StatementNode statement : statements) {
                    statement.execute(robot);
            }
        }
    }

    static class whileNode implements RobotProgramNode{


        blockNode block;
        condNode endCondition;

        public whileNode(blockNode b, condNode c){
            this.block = b;
            this.endCondition = c;
        }

        @Override
        public void execute(Robot robot) {
            while (this.endCondition.evaluate(robot)){
                block.execute(robot);
            }
        }
    }

    static class ifNode implements RobotProgramNode{

        List<ifNode> elif;
        condNode condition;
        blockNode blockNode;
        elseNode elseNode;

        public ifNode(condNode c, blockNode blockNode, elseNode elseNode, List<ifNode> elif){
            this.condition = c;
            this.blockNode = blockNode;
            this.elseNode = elseNode;
            this.elif = elif;
        }

        @Override
        public void execute(Robot robot){
            if (this.condition.evaluate(robot)){
                blockNode.execute(robot);
            }
            else if (elif != null){
                for (ifNode elifNode : elif){
                    elifNode.execute(robot);
                }
            }
            else if (elseNode != null) elseNode.execute(robot);
        }
    }

    static class elseNode implements RobotProgramNode{

        blockNode blockNode;

        public elseNode(blockNode blockNode){
            this.blockNode = blockNode;
        }

        @Override
        public void execute(Robot robot){
            this.blockNode.execute(robot);
        }
    }
    /**
     *  ================= ACTION NODE =================
     */
    static class actionNode implements RobotProgramNode{

        String action;
        argumentNode args;

        public actionNode(String action, argumentNode args){
            this.args = args;
            this.action = action;
        }


        @Override
        public void execute(Robot robot) {
            if (action.equals("move")){
                robot.move();
            }
            else if (action.equals("turnR")){
                robot.turnRight();
            }
            else if (action.equals("turnL")){
                robot.turnLeft();
            }
            else if (action.equals("takeFuel")){
                robot.takeFuel();
            }
            else if (action.equals("turnAround")){
                robot.turnAround();
            }
            else if (action.equals("shieldOn")){
                robot.setShield(true);
            }
            else if (action.equals("shieldOff")){
                robot.setShield(false);
            }
            else{
                robot.idleWait();
            }
        }
    }

    /**
     *  ===================== CALCULATION NODES ====================
     */

    static class relopNode implements RobotConditionalNode{

        String relop;
        argumentNode arg1;
        argumentNode arg2;

        public relopNode(String relop, argumentNode arg1, argumentNode arg2){
            this.relop = relop;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public boolean evaluate(Robot robot) {
            if (this.relop.equals("lt")){
                return arg1.evaluate(robot) < arg2.evaluate(robot);
            }
            else if(this.relop.equals("gt")){
                return arg1.evaluate(robot) > arg2.evaluate(robot);
            }
            else {
                return arg1.evaluate(robot) == arg2.evaluate(robot);
            }
        }
    }
    static class condNode implements RobotConditionalNode {

        relopNode relopNode;
        String operation;
        condNode cond1;
        condNode cond2;

        public condNode(relopNode relopNode, String operation, condNode cond1, condNode cond2) {
            this.relopNode = relopNode;
            this.operation = operation;
            this.cond1 = cond1;
            this.cond2 = cond2;
        }

        @Override
        public boolean evaluate(Robot robot) {
            if (relopNode != null){
                return relopNode.evaluate(robot);
            }
            else {
                if (operation.equals("and")){
                    return cond1.evaluate(robot) && cond2.evaluate(robot);
                }
                else if (operation.equals("or")){
                    return cond1.evaluate(robot) || cond2.evaluate(robot);
                }
                else return !cond1.evaluate(robot);
            }
        }
    }
    static class opNode implements RobotIntegerNode{

        String type;
        argumentNode argNode1;
        argumentNode argNode2;


        public opNode(String type, argumentNode argNode1, argumentNode argNode2){
            this.type = type;
            this.argNode1 = argNode1;
            this.argNode2 = argNode2;
        }

        public int evaluate(Robot robot){
            if (type.equals("add")){
                return argNode1.evaluate(robot) + argNode2.evaluate(robot);
            }
            else if (type.equals("sub")){
                return argNode1.evaluate(robot) - argNode2.evaluate(robot);
            }
            else if (type.equals("mul")){
                return argNode1.evaluate(robot) * argNode2.evaluate(robot);
            }
            else{
                return argNode1.evaluate(robot) / argNode2.evaluate(robot);
            }
        }
    }

    static class argumentNode implements RobotIntegerNode{

        Integer integer;
        senNode sensor;
        opNode opNode;
        variableNode var;

        public argumentNode(Integer integer, senNode sensor, opNode opNode){
            this.integer = integer;
            this.sensor = sensor;
            this.opNode = opNode;
        }

        public int evaluate(Robot robot){
            if (integer != null){
                return integer;
            }
            else if (sensor != null){
                return sensor.evaluate(robot);
            }
            else if (var != null){
                return var.evaluate(robot);
            }
            else return opNode.evaluate(robot);
        }
    }


    static class senNode implements RobotIntegerNode{

        String sensor;

        public senNode(String sensor){
            this.sensor = sensor;
        }

        public int evaluate(Robot robot){
            if (this.sensor.equals("fuelLeft")){
                return robot.getFuel();
            }
            else if (this.sensor.equals("oppLR")){
                return robot.getOpponentLR();
            }
            else if (this.sensor.equals("oppFB")){
                return robot.getOpponentFB();
            }
            else if (this.sensor.equals("numBarrels")){
                return robot.numBarrels();
            }
            else if (this.sensor.equals("barrelLR")){
                return robot.getClosestBarrelLR();
            }
            else if (this.sensor.equals("barrelFB")) {
                return robot.getClosestBarrelFB();
            }
            else return robot.getDistanceToWall();
        }
    }

    static class variableNode implements RobotIntegerNode{

        Integer integer;
        senNode sensor;

        public variableNode(Integer integer, senNode sensor){
            this.integer = integer;
            this.sensor = sensor;
        }

        @Override
        public int evaluate(Robot robot) {
            if (sensor != null){
                return sensor.evaluate(robot);
            }
            else return integer;
        }
    }
}
