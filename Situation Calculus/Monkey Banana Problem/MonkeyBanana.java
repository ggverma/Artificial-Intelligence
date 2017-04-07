import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;

enum Position{
  posa, posb, posc, posd
}

enum Height{
  low, high
}

public class MonkeyBanana{

  String outputStateVariable;

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    System.out.print("Enter the goal: ");

    Monkey monkey = new Monkey();

    Box box = new Box();

    Banana[] bananas = new Banana[3];
    bananas[0] = new Banana(1, Position.posa);
    bananas[1] = new Banana(2, Position.posb);
    bananas[2] = new Banana(3, Position.posc);

    String goal = sc.nextLine();

    new Q3().solveQuery(goal, monkey, bananas, box);
  }

  /*
    Solves user query.
  */
  private void solveQuery(String query, Monkey monkey, Banana[] bananas, Box box){
    try{
      outputStateVariable = null;
      // different state variables like S, S1, S2 are different situations.
      // Each key in this map stores goals corresponding to a particular situation.
      HashMap<String, List<SubGoal>> stateMap = filterInputGoals(query);

      if(stateMap.size() == 0){
        // This is true when all the subgoals were focussed on init and are true.
        System.out.println("true.");
        System.exit(0);
      }

      for(HashMap.Entry<String, List<SubGoal>> stateEntry : stateMap.entrySet()){
        List<SubGoal> subGoalList = stateEntry.getValue();
        SubGoal[] subGoals = new SubGoal[subGoalList.size()];
        subGoals = subGoalList.toArray(subGoals);

        // Check if there's a holds subgoal.
        SubGoal holdsSubGoal = null;
        for(SubGoal subGoal : subGoals){
          if(subGoal.aim.equals("holds")){
            holdsSubGoal = subGoal;
            break;
          }
        }

        if(holdsSubGoal != null){
          // If there's a holds subgoal and an 'at' subgoal for a banana and
          // holds contains the same banana, it means that monkey should be at
          //this position holding this banana.

          // Thus, we can add a  new subGoal for it.

          for(SubGoal subGoal : subGoals){
            if(subGoal.aim.equals("at") && subGoal.isABananaSubgoal){
              int thisBananaId = Integer.parseInt(subGoal.objectName.substring(6));
              if(holdsSubGoal.holdsBananasWithIds.contains(thisBananaId)){
                String subPos = "posa";
                if(subGoal.position == Position.posb) subPos = "posb";
                else if(subGoal.position == Position.posc) subPos = "posc";
                else if(subGoal.position == Position.posd)subPos = "posd";
                String subHeight = "low";
                if(subGoal.height == Height.high) subHeight = "high";
                if(subGoals[subGoals.length - 1].aim.equals("aim") && subGoals[subGoals.length - 1].objectName.equals("monkey")){
                  Position newSubPosition = Position.posa;
                  if(subPos.equals("posb")) newSubPosition = Position.posb;
                  else if(subPos.equals("posc")) newSubPosition = Position.posc;
                  else if(subPos.equals("posd")) newSubPosition = Position.posd;

                  Height newSubHeight = Height.low;
                  if(subHeight.equals("high")) newSubHeight = Height.high;

                  if(subGoals[subGoals.length - 1].position != newSubPosition || subGoals[subGoals.length - 1].height != newSubHeight){
                    System.out.println(subGoal.answerState + "=false.");
                    System.exit(0);
                  }
                }else{
                  subGoalList.add(new SubGoal("monkey", subPos, subHeight, subGoal.answerState));
                }
              }
            }
          }
          // Refresh subGoals if changed.
          if(subGoalList.size() != subGoals.length){
            subGoals = new SubGoal[subGoalList.size()];
            subGoals = subGoalList.toArray(subGoals);
          }
        }

        // Some conditions are obvious in which goal cannot be satisfied.
        // If it requires one of those situation, we do not need to search.
        if(isGoalPossible(subGoals)){
          List<String> steps = new ArrayList<>();
          boolean hasHoldsClause = false;
          // Evaluate each subGoal sequentially.
          for(SubGoal subGoal : subGoals){
            if(subGoal.aim.equals("holds")){
              // This is a holds goal. There may exist a situation when a monkey wants to move the box
              // while he is holding bananas. If that happends, we need to get these bananas back.
              hasHoldsClause = true;
            }
            steps.addAll(evaluateSubGoal(subGoal, bananas, monkey, box, stateEntry.getKey()));
            if(box.isBoxFixed && hasHoldsClause){
              // Get those bananas back.
              for(SubGoal holdsGoal : subGoals){
                if(holdsGoal.aim.equals("holds")){
                  for(int bananaId : holdsGoal.holdsBananasWithIds){
                    if(!monkey.carries.contains(bananas[bananaId - 1]))
                      steps.addAll(getBanana(bananas[bananaId - 1], monkey, box, stateEntry.getKey()));
                  }
                }
              }
              hasHoldsClause = false;
            }
          }
          System.out.println("\n" + stateEntry.getKey() + "=" + formatSteps(steps));
        }else{
          System.out.println(stateEntry.getKey() + "=" + "false.");
        }
        reinitialize(monkey, box, bananas);
      }
    }catch(Exception e){
      // e.printStackTrace();
      System.out.println("Invalid input!");
      System.exit(0);
    }
  }

  /*
    Formats in Prolog format.
  */
  private String formatSteps(List<String> steps){
    // Output in prolog format.
    if(steps.size() == 0) return "init.";
    String result = "do(" + steps.get(0) + ", init)";
    for(int i = 1 ; i < steps.size() ; i++){
      String x = "do(" + steps.get(i) + ", " + result + ")";
      result = x;
    }
    result += ".";
    return result;
  }

  /*
    reinitializes the objects viz. monkey, box, and bananas/
  */
  private void reinitialize(Monkey monkey, Box box, Banana[] bananas){
    // Reload model.
    monkey.position = Position.posa;
    monkey.height = Height.low;
    monkey.carries = new ArrayList<>();

    box.position = Position.posd;
    box.isBoxFixed = false;

    bananas[0].position = Position.posa;
    bananas[0].height = Height.high;

    bananas[1].position = Position.posb;
    bananas[1].height = Height.high;

    bananas[2].position = Position.posc;
    bananas[2].height = Height.high;
  }

  /*
    Evaluates a sub goal.
  */
  private List<String> evaluateSubGoal(SubGoal subGoal, Banana[] bananas, Monkey monkey, Box box, String situation){
    List<String> result = new ArrayList<>();
    if(subGoal.aim.equals("at")){
      // 'at' subgoal
      if(subGoal.isABananaSubgoal){
        // Move banana
        // default banana1
        int toGetBanana = 0;
        if(subGoal.objectName.equals("banana2")){
          // Move banana2
          toGetBanana = 1;
        }else if(subGoal.objectName.equals("banana3")){
          // Move banana3
          toGetBanana = 2;
        }
        if(subGoal.position == bananas[toGetBanana].position && subGoal.height == bananas[toGetBanana].height) return new ArrayList<>();

        // Go to banana and grab it.
        result = getBanana(bananas[toGetBanana], monkey, box, situation);
        if(subGoal.height == Height.high){
          result.addAll(getBoxTo(subGoal.position, box, monkey, situation));
          result.addAll(getBanana(bananas[toGetBanana], monkey, box, situation));
          result.addAll(moveMonkeyTo(subGoal.position, subGoal.height, monkey, box, situation));
        }else{
          result.addAll(moveMonkeyTo(subGoal.position, subGoal.height, monkey, box, situation));
        }
        bananas[toGetBanana].position = subGoal.position;
        bananas[toGetBanana].height = subGoal.height;
        bananas[toGetBanana].isPositionFixed = true;
      }else if(subGoal.objectName.equals("box")){
        // Move box
        if(subGoal.height == Height.high){
          // Box can not reach 'high'.
          System.out.println(situation + "=false.");
          System.exit(0);
        }
        result = getBoxTo(subGoal.position, box, monkey, situation);
        box.isBoxFixed = true;
      }else{
        // Move monkey
        result = moveMonkeyTo(subGoal.position, subGoal.height, monkey, box, situation);
      }
    }else{
      // 'holds' subGoal.
      result = new ArrayList<>();
      while(true){
        // while the monkey does does not hold all valid bananas, keep retrying.
        for(int bananaId : subGoal.holdsBananasWithIds){
          if(!monkey.carries.contains(bananas[bananaId - 1]))
            result.addAll(getBanana(bananas[bananaId - 1], monkey, box, situation));
        }
        if(monkey.carries.size() == subGoal.holdsBananasWithIds.size()) break;
      }
    }
    return result;
  }

  /*
    Make monkey go to banana and grasp it.
  */
  private List<String> getBanana(Banana banana, Monkey monkey, Box box, String situation){
    // steps that monkey will take to get the 'banana' and grasp it in situation 'situation'.
    List<String> steps = new ArrayList<>();
    if(banana.height == Height.low)
      steps = moveMonkeyTo(banana.position, banana.height, monkey, box, situation);
    else{
      steps = getBoxTo(banana.position, box, monkey, situation);
      steps.add("climbup()");
      monkey.height = Height.high;
    }
    steps.add("grasp(banana" + (banana.id) + ")");
    monkey.carries.add(banana);
    return steps;
  }

  /*
    Moves monkey to position 'position' at height 'height' in situation 'situation'.
  */
  private List<String> moveMonkeyTo(Position position, Height height, Monkey monkey, Box box, String situation){
    List<String> steps = new ArrayList<>();
    if(position == monkey.position){
      if(monkey.height != height){
        if(height == Height.high){
          if(box.position != monkey.position){
            steps.addAll(getBoxTo(monkey.position, box, monkey, situation));
          }
          steps.add("climbup()");
          monkey.height = Height.high;
        } else{
          steps.add("climbdown()");
          monkey.height = Height.low;
        }
      }
    }else{
      // He won't carry those bananas that have associated goal state. Leave the bananas in high state, if any wants to.
      List<Banana> toRemoveBananas = new ArrayList<>();
      for(Banana banana : monkey.carries){
        if(banana.isPositionFixed && banana.height == Height.high){
          steps.add("ungrasp(banana" + banana.id + ")");
          banana.position = monkey.position;
          banana.height = Height.low;
          toRemoveBananas.add(banana);
        }
      }
      for(Banana banana : toRemoveBananas){
        monkey.carries.remove(banana);
      }
      // monkey needs to be on the ground in order to travel.
      if(monkey.height == Height.high){
        steps.add("climbdown()");
        monkey.height = Height.low;
      }
      // He won't carry those bananas that have associated goal state in lower position.
      toRemoveBananas = new ArrayList<>();
      for(Banana banana : monkey.carries){
        if(banana.isPositionFixed){
          steps.add("ungrasp(banana" + banana.id + ")");
          banana.position = monkey.position;
          banana.height = Height.low;
          toRemoveBananas.add(banana);
        }
      }
      for(Banana banana : toRemoveBananas){
        monkey.carries.remove(banana);
      }
      steps.add("go("+ position +")");
      // CLimb up, if needed.
      if(height == Height.high){
        steps.addAll(getBoxTo(position, box, monkey, situation));
        steps.add("climbup()");
        monkey.height = Height.high;
      }
    }
    monkey.position = position;
    monkey.height = height;
    return steps;
  }

  /*
    Gets box to position 'position' in situation 'situation'.
  */
  private List<String> getBoxTo(Position position, Box box, Monkey monkey, String situation){
    List<String> steps = new ArrayList<>();
    if(box.position == position) return steps;
    if(box.isBoxFixed){
      System.out.println("\n" + situation + "=false.");
      System.exit(0);
    }
    // Go to box.
    steps.addAll(moveMonkeyTo(box.position, Height.low, monkey, box, situation));
    // Lose all carrying bananas before moving the box.
    List<Banana> toUngraspBananas = new ArrayList<>();
    for(Banana banana : monkey.carries){
      banana.position = box.position;
      banana.height = Height.low;
      steps.add("ungrasp(banana" + banana.id + ")");
      toUngraspBananas.add(banana);
    }
    for(Banana banana : toUngraspBananas){
      monkey.carries.remove(banana);
    }
    // Push it.
    steps.add("push(" + position + ")");
    box.position = position;
    monkey.position = position;
    return steps;
  }

  /*
      Tells if a goal is reachable.
  */
  private boolean isGoalPossible(SubGoal[] subGoals){
    // Monkey holding a banana but both are at different location in a situation. Impossible situation.
    for(SubGoal subGoal : subGoals){
      if(subGoal.aim.equals("holds")){
        for(int j = 0 ; j < subGoals.length ; j ++){
          if(subGoals[j].aim.equals("at") && subGoals[j].isABananaSubgoal){
            for(int i = subGoals.length - 1; i > j ; i --){
              if(subGoals[i].aim.equals("at") && subGoals[i].objectName.equals("monkey")){
                for(int k : subGoal.holdsBananasWithIds)
                  if(("banana" + k).equals(subGoals[j].objectName)){
                    if(subGoals[i].position != subGoals[j].position || subGoals[i].height != subGoals[j].height)
                      return false;
                  }
              }else{
                break;
              }
            }
          }else{
            break;
          }
        }
      }
    }

    // Multiple goals with same objects under configuration are invalid.
    for(int i = 0 ; i < subGoals.length ; i++){
      for(int j = i + 1 ; j < subGoals.length ; j++){
        if(!subGoals[i].aim.equals("holds") && subGoals[i].aim.equals(subGoals[j].aim) && subGoals[i].objectName.equals(subGoals[j].objectName)){
          if(subGoals[i].position != subGoals[j].position) return false;
          if(subGoals[i].height != subGoals[j].height) return false;
        }
      }
    }

    return true;
  }

  /*
    Reads and filters subgoals from raw query.
    NOTE: This parses the query. Please follow correct grammar syntax.
  */
  private HashMap<String, List<SubGoal>> filterInputGoals(String query) throws Exception{
    if(query.charAt(query.length() - 1) != '.'){
      throw new Exception();
    }else{
      query = query.substring(0, query.length() - 1);
    }
    String [] subGoals = query.split("(?<=[)])");

    HashMap<String, List<SubGoal>> map = new HashMap<>();

    for(int i = 0 ; i < subGoals.length ; i++){
      int j = 0;
      while(subGoals[i].charAt(j) == ' ' || subGoals[i].charAt(j) == ','){
        j++;
      }
      subGoals[i] = subGoals[i].substring(j);
    }

    Arrays.parallelSetAll(subGoals, (i) -> subGoals[i].trim());

    int subGoalNumber = 0;

    for(String subGoal : subGoals){
      int i = subGoal.indexOf("(");
      String goalAim = subGoal.substring(0, i);
      goalAim = goalAim.trim();
      if(goalAim.equals("at")){
        // At
        // First parameter
        int j = subGoal.indexOf(",", ++i);
        String parameter = subGoal.substring(i, j);
        String parameter1 = parameter.trim();
        // Second parameter
        i = j + 1;
        while(subGoal.charAt(i) == ' ') i++;
        j = subGoal.indexOf(",", i);
        parameter = subGoal.substring(i, j);
        String parameter2 = parameter.trim();
        // Third parameter
        i = j + 1;
        while(subGoal.charAt(i) == ' ') i++;
        j = subGoal.indexOf(",", i);
        parameter = subGoal.substring(i, j);
        String parameter3 = parameter.trim();
        // Fourth parameter
        i = j + 1;
        while(subGoal.charAt(i) == ' ') i++;
        j = subGoal.indexOf(")", i);
        parameter = subGoal.substring(i, j);
        String parameter4 = parameter.trim();

        if(parameter4.equals("init")){
          SubGoal initGoal = new SubGoal(parameter1, parameter2, parameter3, parameter4);

          if(initGoal.aim.equals("at")){
            if(initGoal.objectName.equals("banana1")){
              if(initGoal.position != Position.posa || initGoal.height != Height.high){
                // Fail.
                System.out.println("false.");
                System.exit(0);
              }
            }else if(initGoal.objectName.equals("banana2")){
              if(initGoal.position != Position.posb || initGoal.height != Height.high){
                // Fail.
                System.out.println("false.");
                System.exit(0);
              }
            }else if(initGoal.objectName.equals("banana3")){
              if(initGoal.position != Position.posc || initGoal.height != Height.high){
                // Fail.
                System.out.println("false.");
                System.exit(0);
              }
            }else if(initGoal.objectName.equals("monkey")){
              if(initGoal.position != Position.posa || initGoal.height != Height.low){
                // Fail.
                System.out.println("false.");
                System.exit(0);
              }
            }else{//box
              if(initGoal.position != Position.posd || initGoal.height != Height.low){
                // Fail.
                System.out.println("false.");
                System.exit(0);
              }
            }
          }else{
            // holds
            if(initGoal.holdsBananasWithIds.size() != 0){
              // Fail.
              System.out.println("false.");
              System.exit(0);
            }
          }

        }else{
          if(!map.containsKey(parameter4)){
            map.put(parameter4, new ArrayList<>());
          }
          List<SubGoal> existingGoals = map.get(parameter4);
          existingGoals.add(new SubGoal(parameter1, parameter2, parameter3, parameter4));
          map.put(parameter4, existingGoals);
        }
      }else if(goalAim.equals("holds")){
        // Holds
        StringBuilder sb = new StringBuilder();
        List<Integer> holdsBananas = new ArrayList<>();
        while(subGoal.charAt(i) != '[') i++;
        i++;
        while(subGoal.charAt(i) != ']'){
          if(subGoal.charAt(i) == ' ' || subGoal.charAt(i) == ','){
            i++;
          }else{
            while(subGoal.charAt(i) != ',' && subGoal.charAt(i) != ' ' && subGoal.charAt(i) != ']'){
              sb.append(subGoal.charAt(i));
              i++;
            }
            if(sb.toString().equals("banana1")){
              holdsBananas.add(1);
            }else if(sb.toString().equals("banana2")){
              holdsBananas.add(2);
            }else if(sb.toString().equals("banana3")){
              holdsBananas.add(3);
            }else{
              System.out.println("Invalid holds parameters.");
              System.exit(0);
            }
          }
          sb = new StringBuilder();
        }
        i++;
        while(subGoal.charAt(i) == ' ') i++;
        i++;
        while(subGoal.charAt(i) == ' ') i++;
        int j = subGoal.indexOf(")", i);
        String parameter2 = subGoal.substring(i, j);
        parameter2 = parameter2.trim();
        if(parameter2.indexOf(",") != -1){
          // Invalid holds clause.
          // Fail.
          System.out.println("ERROR: invalid holds clause.");
          System.exit(0);
        }
        if(parameter2.equals("init")){
          if(holdsBananas.size() != 0){
            // Fail.
            System.out.println("false.");
            System.exit(0);
          }
        }else{
          if(!map.containsKey(parameter2)){
            map.put(parameter2, new ArrayList<>());
            List<SubGoal> existingGoals = map.get(parameter2);
            existingGoals.add(new SubGoal(holdsBananas));
            map.put(parameter2, existingGoals);
          }else{
            // Check if already an holds goal exists.
            boolean holsGoalAreadyExists = false;
            for(SubGoal goal : map.get(parameter2)){
              if(goal.aim.equals("holds")){
                // Multple holds clause merge into one.
                holsGoalAreadyExists = true;
                List<Integer> areadyHolds = goal.holdsBananasWithIds;
                areadyHolds.addAll(holdsBananas);
                break;
              }
            }
            if(!holsGoalAreadyExists){
              List<SubGoal> existingGoals = map.get(parameter2);
              existingGoals.add(new SubGoal(holdsBananas));
              map.put(parameter2, existingGoals);
            }
          }
        }
      }else{
        System.out.println("Invalid grammar.");
        System.exit(0);
      }
      subGoalNumber++;
    }

    // Prioritize goals to make optimal solution.
    for(List<SubGoal> subGoalList :  map.values()){
      Collections.sort(subGoalList);
    }
    return map;
  }
}

class Monkey{
  Height height;
  Position position;
  List<Banana> carries;

  public Monkey(){
    position = Position.posa;
    carries = new ArrayList<>();
    height = Height.low;
  }
}

class Banana{
  int id;
  Height height;
  Position position;

  // Tells if this banana is associated with a goal of type 'at'.
  boolean isPositionFixed;

  public Banana(int id, Position p){
    this.id = id;
    height = Height.high;
    position = p;

    isPositionFixed = false;
  }

  public String toString(){
    return "banana" + (id - 1);
  }
}

class Box{
  Height height;
  Position position;

  // Becomes true after when an 'at' goal for box exists.
  boolean isBoxFixed;

  public Box(){
    position = Position.posd;
    height = Height.low;
    isBoxFixed = false;
  }
}

class SubGoal implements Comparable<SubGoal>{
  // Helps in prioritizing the goal.
  int sortingFactor;

  String aim;
  String objectName;
  Position position;
  Height height;

  // The variable for situation. like S.
  String answerState;

  boolean isABananaSubgoal;

  List<Integer> holdsBananasWithIds;

  public SubGoal(String p1, String p2, String p3, String p4){
    aim = "at";
    isABananaSubgoal = false;
    if(p1.equals("banana1")){
      objectName = "banana1";
      sortingFactor = -10;
      isABananaSubgoal = true;
    }
    else if(p1.equals("banana2")){
      objectName = "banana2";
      sortingFactor = -9;
      isABananaSubgoal = true;
    }
    else if(p1.equals("banana3")){
      objectName = "banana3";
      sortingFactor = -8;
      isABananaSubgoal = true;
    }
    else if(p1.equals("monkey")){
      objectName = "monkey";
      sortingFactor = 10;
    }
    else if(p1.equals("box")){
      objectName = "box";
      sortingFactor = 9;
    }else {
      System.out.println("Invalid object in 'at' parameters.");
      System.exit(0);
    }
    if(p2.equals("posa")){
      position = Position.posa;
    }else if(p2.equals("posb")){
      position = Position.posb;
    }else if(p2.equals("posc")){
      position = Position.posc;
    }else if(p2.equals("posd")){
      position = Position.posd;
    }else{
      System.out.println("Invalid position in 'at' parameters.");
      System.exit(0);
    }

    if(p3.equals("low")){
      height = height.low;
    }else if(p3.equals("high")){
      height = height.high;
    }else{
      System.out.println("Invalid height in 'at' parameters.");
      System.exit(0);
    }

    if(p4.charAt(0) >= 'A' && p4.charAt(0) <= 'Z'){
      answerState = p4;
    }else if(p4.equals("init")){
      answerState = p4;
      // Evaluate these first.
      sortingFactor = -20;
    }else{
      System.out.println("Invalid situation argument in 'at' parameters.");
      System.exit(0);
    }
  }

  public SubGoal(List<Integer> hb){
    aim = "holds";
    holdsBananasWithIds = hb;
    sortingFactor = 0;
  }

  public int compareTo(SubGoal otherGoal){
    if(sortingFactor < otherGoal.sortingFactor) return -1;
    else if(sortingFactor == otherGoal.sortingFactor) return 0;
    else return 1;
  }

  public String toString(){
    if( aim.equals("at")) return "at(" + objectName + ", " + position + ", " + height + ", S)";
    else{
      String repr = "holds([";
      for(int i = 0 ; i < holdsBananasWithIds.size() - 1 ; i++){
        repr += "banana" + holdsBananasWithIds.get(i) + ", ";
      }
      repr += "banana" + holdsBananasWithIds.get(holdsBananasWithIds.size() - 1) + "], S)";
      return repr;
    }
  }
}
