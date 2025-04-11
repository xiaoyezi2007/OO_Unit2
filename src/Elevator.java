import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.TimableOutput;
import static java.lang.Thread.sleep;

public class Elevator implements Runnable {
    private int id;
    private Floor floor;
    private int doorOpen;
    private int size;
    private ElevatorQueue elevatorQueue;
    private int direction;// 0 = stop ; 1 = up ; -1 = down
    private int arrDirection;
    private Floor targetFloor = null;
    private SameWell sameWell = null;

    public Elevator(int id, ElevatorQueue elevatorQueue) {
        floor = new Floor("F1");
        this.id = id;
        doorOpen = 0;
        size = 6;
        this.elevatorQueue = elevatorQueue;
        direction = 0;
        elevatorQueue.setNowFloor(floor);
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    }

    public void print(String op, Floor floor, int elevatorId, int personId,
        boolean scheCode) { // true = begin,false = end
        if (op.equals("ARRIVE") || op.equals("OPEN") || op.equals("CLOSE")) {
            TimableOutput.println(
                op + "-" + floor.toString() + "-" + elevatorId
            );
        }
        else if (op.equals("SCHE")) {
            if (scheCode) {
                TimableOutput.println(
                    op + "-BEGIN-" + elevatorId
                );
            }
            else {
                TimableOutput.println(
                    op + "-END-" + elevatorId
                );
            }
        }
        else {
            TimableOutput.println(
                op + "-" + personId + "-" + floor.toString() + "-" + elevatorId
            );
        }
    }

    @Override
    public void run() {
        while (true) {
            if (elevatorQueue.isEnd() && elevatorQueue.isEmpty() && doorOpen == 0) {
                break;
            }
            String op = operation();
            switch (op) {
                case "sche":
                    Sche();
                    break;
                case "update":
                    Update();
                    break;
                case "wait":
                    break;
                case "up":
                    Up();
                    break;
                case "down":
                    Down();
                    break;
                case "open":
                    Open();
                    break;
                case "close":
                    Close();
                    break;
                default:
            }
        }
    }

    public synchronized String operation() {
        String op = "None";
        if (elevatorQueue.needSche()) {
            op = "sche";
        }
        else if (elevatorQueue.needUpdate()) {
            op = "update";
        }
        else if (doorOpen == 1) {
            op = "close";
        }
        else if (sameWell != null && floor.equals(targetFloor) && elevatorQueue.havePersonIn()
            && arrDirection == -direction) {
            op = "open";
        }
        else if (sameWell != null && sameWell.moveAway(id) != 0 && floor.equals(targetFloor)) {
            if (sameWell.moveAway(id) == 1) {
                direction = 1;
                op = "up";
            }
            else {
                direction = -1;
                op = "down";
            }
        }
        else if (elevatorQueue.emptyWait()) {
            op = "wait";
        }
        else if (elevatorQueue.isArrive(floor)) {
            op = "open";
        }
        else {
            int newDirection = elevatorQueue.dir(floor);
            if (elevatorQueue.hopeIn(newDirection, floor, size)) {
                op = "open";
            }
            else if (newDirection == 1) {
                op = "up";
            }
            else if (newDirection == -1) {
                op = "down";
            }
            direction = newDirection;
        }
        notifyAll();
        return op;
    }

    public void Up() {
        if (elevatorQueue.needSche() || elevatorQueue.maySche() || elevatorQueue.mayUpdate()) {
            return;
        }
        if (sameWell != null && floor.upFloor().equals(targetFloor)) {
            sameWell.moveIn();
        }
        floor.up();
        elevatorQueue.setNowFloor(floor);
        try {
            if (sameWell == null) {
                sleep(400);
            }
            else {
                sleep(200);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        print("ARRIVE", floor, id, -1, true);
        if (sameWell != null && floor.downFloor().equals(targetFloor)) {
            sameWell.away();
        }
    }

    public void Down() {
        if (elevatorQueue.needSche() || elevatorQueue.maySche() || elevatorQueue.mayUpdate()) {
            return;
        }
        if (sameWell != null && floor.downFloor().equals(targetFloor)) {
            sameWell.moveIn();
        }
        floor.down();
        elevatorQueue.setNowFloor(floor);
        try {
            if (sameWell == null) {
                sleep(400);
            }
            else {
                sleep(200);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        print("ARRIVE", floor, id, -1, true);
        if (sameWell != null && floor.upFloor().equals(targetFloor)) {
            sameWell.away();
        }
    }

    public void Open() {
        doorOpen = 1;
        print("OPEN", floor, id, -1, true);
        try {
            sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void Close() {
        doorOpen = 0;
        if (sameWell != null && floor.equals(targetFloor) && elevatorQueue.havePersonIn()
            && arrDirection == -direction) {
            elevatorQueue.personOutAnyway(id, floor, false);
        }
        else {
            elevatorQueue.personOut(id, floor);
        }
        elevatorQueue.personExchange(id, size, floor, direction);
        print("CLOSE", floor, id, -1, true);
    }

    public void Sche() {
        PrepareSche();
        ScheRequest scheRequest = elevatorQueue.removeScheRequest();
        final double speed = scheRequest.getSpeed();
        final Floor destination = new Floor(scheRequest.getToFloor());
        elevatorQueue.setSilence(true);
        print("SCHE", floor, id, -1, true);
        elevatorQueue.removeWaitingPerson();
        while (floor.goUp(destination)) {
            try {
                sleep((long) (speed * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            floor.up();
            elevatorQueue.setNowFloor(floor);
            print("ARRIVE", floor, id, -1, true);
        }
        while (destination.goUp(floor)) {
            try {
                sleep((long) (speed * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            floor.down();
            elevatorQueue.setNowFloor(floor);
            print("ARRIVE", floor, id, -1, true);
        }
        doorOpen = 1;
        print("OPEN", floor, id, -1, true);
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        elevatorQueue.personOutAnyway(id, floor, true);
        doorOpen = 0;
        print("CLOSE", floor, id, -1, true);
        print("SCHE", floor, id, -1, false);
        elevatorQueue.setSilence(false);
        direction = 0;
    }

    public void PrepareSche() {
        if (doorOpen == 0 && elevatorQueue.needPrepareSche(floor)) {
            Open();
        }
        elevatorQueue.prepareSche(floor, size);
        if (doorOpen == 1) {
            doorOpen = 0;
            print("CLOSE", floor, id, -1, true);
        }
    }

    public void Update() {
        elevatorQueue.setSilence(true);
        elevatorQueue.personOutAnyway(id, floor, true);
        this.sameWell = elevatorQueue.getSameWell();
        sameWell.ready();
        while (!sameWell.allReadyUpdate()) {
            try {
                sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.floor = sameWell.update(id);
        elevatorQueue.setNowFloor(floor);
        this.targetFloor = sameWell.getTargetFloor();
        elevatorQueue.setTargetFloor(targetFloor);
        this.arrDirection = sameWell.getArrDirection(id);
        elevatorQueue.setArrDirection(arrDirection);
        direction = 0;
        elevatorQueue.setSilence(false);
    }
}
