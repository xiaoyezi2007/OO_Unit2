import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;

import java.util.ArrayList;
import java.util.HashMap;

public class Judge {
    private final ElevatorQueue elevatorQueue;

    public Judge(ElevatorQueue elevatorQueue) {
        this.elevatorQueue = elevatorQueue;
    }

    public synchronized int dirOfEmpty(Floor floor, HashMap<Floor,
        ArrayList<PersonRequest>> personRequests) {
        Floor tmp = floor.clone();
        int upPriority = 0;
        Floor highest = new Floor("F7");
        Floor highFloor = floor.clone();
        tmp.up();
        while (tmp.goUp(highest) || tmp.equals(highest)) {
            for (PersonRequest personRequest : personRequests.get(tmp)) {
                if (highFloor.goUp(tmp)) {
                    highFloor = tmp.clone();
                }
                if (highFloor.goUp(new Floor(personRequest.getToFloor()))) {
                    highFloor = new Floor(personRequest.getToFloor());
                }
                upPriority += personRequest.getPriority();
            }
            tmp.up();
        }
        tmp = floor.clone();
        tmp.down();
        int downPriority = 0;
        Floor lowest = new Floor("B4");
        Floor lowFloor = floor.clone();
        while (lowest.goUp(tmp) || tmp.equals(lowest)) {
            for (PersonRequest personRequest : personRequests.get(tmp)) {
                if (tmp.goUp(lowFloor)) {
                    lowFloor = tmp.clone();
                }
                if (new Floor(personRequest.getToFloor()).goUp(lowFloor)) {
                    lowFloor = tmp.clone();
                }
                downPriority += personRequest.getPriority();
            }
            tmp.down();
        }
        for (PersonRequest personRequest : personRequests.get(floor)) {
            Floor fromFloor = new Floor(personRequest.getFromFloor());
            Floor toFloor = new Floor(personRequest.getToFloor());
            if (toFloor.goUp(fromFloor)) {
                downPriority += personRequest.getPriority();
                if (toFloor.goUp(lowFloor)) {
                    lowFloor = toFloor.clone();
                }
            }
            else {
                upPriority += personRequest.getPriority();
                if (highFloor.goUp(toFloor)) {
                    highFloor = toFloor.clone();
                }
            }
        }
        return solveDirOfEmpty(upPriority, downPriority, floor, lowFloor, highFloor);
    }

    public synchronized int solveDirOfEmpty(int upPriority, int downPriority, Floor floor,
        Floor lowFloor, Floor highFloor) {
        if (upPriority * floor.minus(lowFloor) > downPriority * highFloor.minus(floor)
            || floor.equals(lowFloor)) {
            elevatorQueue.setForceDirection(1);
            return 1;
        }
        else {
            elevatorQueue.setForceDirection(-1);
            return -1;
        }
    }

    public synchronized boolean needPrepareSche(Floor floor, ArrayList<PersonRequest> personIn,
        HashMap<Floor, ArrayList<PersonRequest>> personRequests,
        ArrayList<ScheRequest> scheRequests) {
        ScheRequest scheRequest = scheRequests.get(0);
        Floor toFloor = new Floor(scheRequest.getToFloor());
        for (PersonRequest personRequest : personIn) {
            Floor destination = new Floor(personRequest.getToFloor());
            if (destination.equals(toFloor) || toFloor.midOf(floor, destination)) {
                continue;
            }
            else {
                return true;
            }
        }
        for (PersonRequest personRequest : personRequests.get(floor)) {
            Floor destination = new Floor(personRequest.getToFloor());
            if (destination.equals(toFloor) || toFloor.midOf(floor, destination)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isSameDirWithElevator(Person person, int direction
        , Floor floor) {
        Floor toFloor = new Floor(person.getToFloor());
        Floor fromFloor = person.getFloorNow();
        int dir = fromFloor.direction(toFloor);
        if (dir == direction) {
            return (direction == -1 && fromFloor.goUp(floor))
                    || (direction == 1 && floor.goUp(fromFloor)) || floor.equals(fromFloor);
        }
        return false;
    }

    public synchronized boolean dirIsFull(Floor minFloor, Floor maxFloor,
        ArrayList<PersonRequest> personIn, Person person,
        Floor nowFloor, HashMap<Floor, ArrayList<PersonRequest>> personRequests) {
        if (minFloor == null || maxFloor == null) {
            return false;
        }
        Floor floor = nowFloor;
        int direction = elevatorQueue.dir(floor);
        if (!isSameDirWithElevator(person, direction, nowFloor)) {
            return true;
        }
        Floor tmp = floor.clone();
        HashMap<Floor, Integer> outCnt = new HashMap<>();
        Floor lowest = new Floor("B4");
        Floor highest = new Floor("F7");
        int num = personIn.size();
        while (lowest.goUp(highest) || lowest.equals(highest)) {
            outCnt.put(lowest.clone(), 0);
            lowest.up();
        }
        for (PersonRequest personRequest : personIn) {
            Floor toFloor = new Floor(personRequest.getToFloor());
            outCnt.put(toFloor, outCnt.get(toFloor) + 1);
        }
        while ((direction == 1 && (tmp.goUp(maxFloor) || tmp.equals(maxFloor)))
                || (direction == -1 && (minFloor.goUp(tmp) || tmp.equals(minFloor)))) {
            for (PersonRequest personRequest : personRequests.get(tmp)) {
                Floor toFloor = new Floor(personRequest.getToFloor());
                if (tmp.goUp(toFloor)) {
                    num++;
                    outCnt.put(toFloor, outCnt.get(toFloor) + 1);
                }
            }
            num -= outCnt.get(tmp);
            if (num > 6) {
                return true;
            }
            if (direction == -1) {
                tmp.down();
            }
            else {
                tmp.up();
            }
        }
        return false;
    }

    public synchronized int countSameDirection(int direction, Floor floor,
        HashMap<Floor, ArrayList<PersonRequest>> personRequests) {
        int cnt = 0;
        for (PersonRequest personRequest : personRequests.get(floor)) {
            Floor tofloor = new Floor(personRequest.getToFloor());
            if (direction  == floor.direction(tofloor)) {
                cnt++;
            }
        }
        return cnt;
    }

    public synchronized int dir(Floor floor, int personCnt,
        int personDirection, Floor minFloor, Floor maxFloor) {
        if (personCnt == 0) {
            elevatorQueue.setForceDirection(0);
            return 0;
        }
        if (personDirection != 0) {
            elevatorQueue.setForceDirection(0);
            return personDirection;
        }
        if (floor.goUp(minFloor)) {
            elevatorQueue.setForceDirection(0);
            return 1;
        }
        if (maxFloor.goUp(floor)) {
            elevatorQueue.setForceDirection(0);
            return -1;
        }
        if (floor.equals(minFloor)) {
            int ans = elevatorQueue.getForceDirection();
            elevatorQueue.setForceDirection(0);
            if (ans == 0) {
                if (elevatorQueue.countSameDirection(-1, floor) > 0) {
                    return -1;
                }
                else {
                    return 1;
                }
            }
            if (elevatorQueue.countSameDirection(ans, floor) > 0) {
                return ans;
            }
            else {
                return -ans;
            }
        }
        if (floor.equals(maxFloor)) {
            int ans = elevatorQueue.getForceDirection();
            elevatorQueue.setForceDirection(0);
            if (ans == 0) {
                if (elevatorQueue.countSameDirection(1, floor) > 0) {
                    return 1;
                }
                else {
                    return -1;
                }
            }
            if (elevatorQueue.countSameDirection(ans, floor) > 0) {
                return ans;
            }
            else {
                return -ans;
            }
        }
        if (elevatorQueue.getForceDirection() != 0) {
            return elevatorQueue.getForceDirection();
        }
        return elevatorQueue.dirOfEmpty(floor);
    }

    public synchronized boolean hopeIn(int direction, Floor floor, int size,
        HashMap<Floor, ArrayList<PersonRequest>> personRequests) {
        if (elevatorQueue.isFull(size)) {
            return false;
        }
        for (PersonRequest personRequest : personRequests.get(floor)) {
            Floor fromFloor = new Floor(personRequest.getFromFloor());
            Floor toFloor = new Floor(personRequest.getToFloor());
            if (direction * fromFloor.direction(toFloor) >= 0) {
                return true;
            }
        }
        return false;
    }
}
