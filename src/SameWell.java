import com.oocourse.elevator3.TimableOutput;

import static java.lang.Thread.sleep;

public class SameWell {
    private ElevatorQueue elevatorA;
    private ElevatorQueue elevatorB;
    private Floor targetFloor;
    private int updateReady = 0;
    private boolean updated = false;
    private boolean busy = false;
    private boolean quickly = false;

    public SameWell(ElevatorQueue a, ElevatorQueue b, String targetFloor) {
        this.elevatorA = a;
        this.elevatorB = b;
        this.targetFloor = new Floor(targetFloor);
    }

    public synchronized void ready() {
        updateReady++;
        notifyAll();
    }

    public synchronized int getArrDirection(int id) {
        if (id == elevatorA.getId()) {
            return 1;
        }
        return -1;
    }

    public synchronized boolean allReadyUpdate() {
        notifyAll();
        return updateReady == 2;
    }

    public synchronized Floor update(int id) {
        if (!updated) {
            TimableOutput.println(
                "UPDATE-BEGIN-" + elevatorA.getId() + "-" + elevatorB.getId()
            );
            elevatorA.removeWaitingPerson();
            elevatorB.removeWaitingPerson();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            TimableOutput.println(
                "UPDATE-END-" + elevatorA.getId() + "-" + elevatorB.getId()
            );
        }
        updated = true;
        notifyAll();
        if (id == elevatorA.getId()) {
            return targetFloor.upFloor();
        }
        else {
            return targetFloor.downFloor();
        }
    }

    public synchronized Floor getTargetFloor() {
        return targetFloor;
    }

    public synchronized boolean isBusy() {
        notifyAll();
        return busy;
    }

    public synchronized boolean isQuickly() {
        notifyAll();
        return quickly;
    }

    public synchronized void moveIn() {
        while (busy) {
            quickly = true;
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        busy = true;
        quickly = false;
        notifyAll();
    }

    public synchronized void setQuickly(boolean quickly) {
        if (quickly) {
            if (busy) {
                this.quickly = true;
            }
        }
        else {
            this.quickly = false;
        }
        notifyAll();
    }

    public synchronized int moveAway(int id) {
        if (quickly) {
            if (id == elevatorA.getId()) {
                return 1;
            }
            else {
                return -1;
            }
        }
        return 0;
    }

    public synchronized void away() {
        busy = false;
        quickly = false;
        notifyAll();
    }

}
