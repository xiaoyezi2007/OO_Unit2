import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.TimableOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class ElevatorQueue {
    private HashMap<Floor, ArrayList<PersonRequest>> personRequests = new HashMap<>();
    private ArrayList<PersonRequest> personIn = new ArrayList<>();
    private ArrayList<ScheRequest> scheRequests = new ArrayList<>();
    private SameWell sameWell;
    private RequestQueue requestQueue;
    private boolean isEnd = false;
    private int personCnt = 0;
    private Floor maxFloor = null;
    private Floor minFloor = null;
    private Floor nowFloor = new Floor("F1");
    private Floor targetFloor = null;
    private int arrDirection = 0;
    private int personDirection = 0;
    private int forceDirection = 0;
    private int id;
    private boolean silence = false;
    private Judge judge;

    public ElevatorQueue(int id, RequestQueue requestQueue) {
        Floor floor = new Floor("B4");
        Floor highest = new Floor("F7");
        while (floor.goUp(highest) || floor.equals(highest)) {
            personRequests.put(floor.clone(), new ArrayList<>());
            floor.up();
        }
        this.id = id;
        this.requestQueue = requestQueue;
        judge = new Judge(this);
    }

    public synchronized boolean takeable(Person person) {
        if (targetFloor == null) {
            return true;
        }
        Floor fromFloor = person.getFloorNow();
        Floor toFloor = new Floor(person.getRequest().getToFloor());
        if (arrDirection == 1) {
            if (targetFloor.goUp(fromFloor)) {
                return true;
            }
            else if (targetFloor.equals(fromFloor)) {
                return targetFloor.goUp(toFloor);
            }
            return false;
        }
        else if (arrDirection == -1) {
            if (fromFloor.goUp(targetFloor)) {
                return true;
            }
            else if (fromFloor.equals(targetFloor)) {
                return toFloor.goUp(targetFloor);
            }
            return false;
        }
        return false;
    }

    public synchronized boolean arriveable(Person person) {
        if (targetFloor == null || personCnt >= 12) {
            return false;
        }
        Floor fromFloor = person.getFloorNow();
        Floor toFloor = new Floor(person.getRequest().getToFloor());
        if (arrDirection == 1) {
            return (fromFloor.equals(targetFloor) || targetFloor.goUp(fromFloor)) &&
                    (toFloor.equals(targetFloor) || targetFloor.goUp(toFloor));
        }
        else if (arrDirection == -1) {
            return (fromFloor.equals(targetFloor) || fromFloor.goUp(targetFloor)) &&
                    (toFloor.equals(targetFloor) || toFloor.goUp(targetFloor));
        }
        return false;
    }

    public synchronized void setArrDirection(int arrDirection) {
        this.arrDirection = arrDirection;
        notifyAll();
    }

    public synchronized int getId() {
        notifyAll();
        return id;
    }

    public synchronized void setTargetFloor(Floor targetFloor) {
        this.targetFloor = targetFloor;
        notifyAll();
    }

    public synchronized boolean maySche() {
        if (!requestQueue.scheEmpty()) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        notifyAll();
        return !requestQueue.scheEmpty();
    }

    public synchronized boolean mayUpdate() {
        if (!requestQueue.updateEmpty()) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        notifyAll();
        return !requestQueue.updateEmpty();
    }

    public synchronized ScheRequest removeScheRequest() {
        notifyAll();
        return scheRequests.remove(0);
    }

    public synchronized SameWell getSameWell() {
        SameWell ans = sameWell;
        sameWell = null;
        notifyAll();
        return ans;
    }

    public synchronized void setNowFloor(Floor floor) {
        nowFloor = floor.clone();
    }

    public synchronized void setSilence(boolean silence) {
        this.silence = silence;
        notifyAll();
    }

    public synchronized boolean isSilence() {
        notifyAll();
        return silence;
    }

    public synchronized boolean dirIsFull(Person person) {
        notifyAll();
        if (targetFloor != null) {
            return true;
        }
        return judge.dirIsFull(minFloor, maxFloor, personIn, person, nowFloor, personRequests);
    }

    public synchronized boolean addPersonRequest(Person person) {
        if (isSilence() || !takeable(person) || personCnt >= 12) {
            return false;
        }
        TimableOutput.println(
            "RECEIVE-" + person.getPersonId() + "-" + id
        );
        Floor floor = person.getFloorNow();
        personRequests.get(floor).add(person.getRequest());
        personCnt++;
        if (maxFloor == null || maxFloor.goUp(floor)) {
            maxFloor = floor.clone();
        }
        if (minFloor == null || floor.goUp(minFloor)) {
            minFloor = floor.clone();
        }
        notifyAll();
        return true;
    }

    public synchronized void addScheRequest(ScheRequest scheRequest) {
        scheRequests.add(scheRequest);
        notifyAll();
    }

    public synchronized void addUpdateRequest(SameWell sameWell) {
        this.sameWell = sameWell;
        notifyAll();
    }

    public synchronized boolean emptyWait() {
        if (personCnt == 0 && isEnd) {
            return false;
        }
        if (personIn.isEmpty() && personCnt == 0 && scheRequests.isEmpty() && sameWell == null) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    public synchronized boolean needUpdate() {
        notifyAll();
        return sameWell != null;
    }

    public synchronized boolean needSche() {
        notifyAll();
        return !scheRequests.isEmpty();
    }

    public synchronized boolean needPrepareSche(Floor floor) {
        return judge.needPrepareSche(floor, personIn, personRequests, scheRequests);
    }

    public synchronized void prepareSche(Floor floor, int size) {
        ScheRequest scheRequest = scheRequests.get(0);
        Floor toFloor = new Floor(scheRequest.getToFloor());
        ArrayList<PersonRequest> delete = new ArrayList<>();
        for (PersonRequest personRequest : personIn) {
            Floor destination = new Floor(personRequest.getToFloor());
            if (!(destination.equals(toFloor) || toFloor.midOf(floor, destination))) {
                delete.add(personRequest);
                if (destination.equals(floor)) {
                    personCnt--;
                    print("OUT", floor, id, personRequest.getPersonId(), true);
                }
                else {
                    personCnt--;
                    print("OUT", floor, id, personRequest.getPersonId(), false);
                    requestQueue.addRequest(new Person(personRequest, floor.clone()));
                }
            }
        }
        for (PersonRequest personRequest : delete) {
            personIn.remove(personRequest);
        }
        delete.clear();
        for (PersonRequest personRequest : personRequests.get(floor)) {
            Floor destination = new Floor(personRequest.getToFloor());
            if (personIn.size() < size &&
                (destination.equals(toFloor) || toFloor.midOf(floor, destination))) {
                personIn.add(personRequest);
                delete.add(personRequest);
                personDirection = floor.direction(destination);
                print("IN", floor, id, personRequest.getPersonId(), false);
            }
        }
        for (PersonRequest personRequest : personIn) {
            personRequests.get(floor).remove(personRequest);
        }
        for (PersonRequest personRequest : delete) {
            personRequests.get(floor).remove(personRequest);
        }
        if (personIn.isEmpty()) {
            personDirection = 0;
        }
        updateMFloor();
    }

    public synchronized void updateMFloor() {
        if (personIn.size() == personCnt) {
            minFloor = null;
            maxFloor = null;
            return;
        }
        Floor tmp = new Floor("B4"); // there is a Constant
        while (personRequests.get(tmp).isEmpty()) {
            tmp = tmp.upFloor();
        }
        minFloor = tmp;
        tmp = new Floor("F7");
        while (personRequests.get(tmp).isEmpty()) {
            tmp = tmp.downFloor();
        }
        maxFloor = tmp;
    }

    public void print(String op, Floor floor, int elevatorId, int personId, boolean isArrived) {
        if (op.equals("ARRIVE") || op.equals("OPEN") || op.equals("CLOSE")) {
            TimableOutput.println(
                op + "-" + floor.toString() + "-" + elevatorId
            );
        }
        else if (op.equals("OUT")) {
            if (isArrived) {
                TimableOutput.println(
                    op + "-S-" + personId + "-" + floor.toString() + "-" + elevatorId
                );
            }
            else {
                TimableOutput.println(
                    op + "-F-" + personId + "-" + floor.toString() + "-" + elevatorId
                );
            }
        }
        else if (op.equals("IN")) {
            TimableOutput.println(
                op + "-" + personId + "-" + floor.toString() + "-" + elevatorId
            );
        }
    }

    public synchronized void personOut(int id, Floor floor) {
        ArrayList<PersonRequest> delete = new ArrayList<>();
        for (PersonRequest personRequest : personIn) {
            if (floor.equals(new Floor(personRequest.getToFloor()))) {
                delete.add(personRequest);
                personCnt--;
                print("OUT", floor, id, personRequest.getPersonId(), true);
            }
        }
        for (PersonRequest personRequest : delete) {
            personIn.remove(personRequest);
        }
        if (!delete.isEmpty()) {
            updateMFloor();
        }
        if (personIn.isEmpty()) {
            personDirection = 0;
        }
        if (isEmpty()) {
            notifyAll();
        }
    }

    public synchronized void personOutAnyway(int id, Floor floor, boolean removeWaiting) {
        for (PersonRequest personRequest : personIn) {
            personCnt--;
            if (!floor.equals(new Floor(personRequest.getToFloor()))) {
                print("OUT", floor, id, personRequest.getPersonId(), false);
                requestQueue.addRequest(new Person(personRequest, floor.clone()));
            }
            else {
                print("OUT", floor, id, personRequest.getPersonId(), true);
            }
        }
        personIn.clear();
        personDirection = 0;
        if (removeWaiting) {
            removeWaitingPerson();
            forceDirection = 0;
        }
        notifyAll();
    }

    public synchronized void removeWaitingPerson() {
        Floor lowest = new Floor("B4");
        Floor highest = new Floor("F7");
        while (lowest.goUp(highest) || lowest.equals(highest)) {
            for (PersonRequest personRequest : personRequests.get(lowest)) {
                requestQueue.addRequest(new Person(personRequest, lowest.clone()));
            }
            personRequests.get(lowest).clear();
            lowest.up();
        }
        personCnt = personIn.size();
        forceDirection = 0;
        updateMFloor();
        notifyAll();
    }

    public synchronized void personExchange(int id, int size, Floor floor, int eleDir) {
        ArrayList<PersonRequest> tmp = new ArrayList<>(personIn);
        ArrayList<PersonRequest> newIn = new ArrayList<>();
        tmp.addAll(personRequests.get(floor));
        for (int i = 1; i <= size; i++) {
            int maxPri = -1;
            PersonRequest maxPriRequest = null;
            for (PersonRequest personRequest : tmp) {
                Floor fromFloor = new Floor(personRequest.getFromFloor());
                Floor toFloor = new Floor(personRequest.getToFloor());
                int direction = fromFloor.direction(toFloor);
                int priority = personRequest.getPriority();
                if (eleDir * direction != -1 && priority > maxPri) {
                    maxPriRequest = personRequest;
                    maxPri = priority;
                }
            }
            if (maxPri == -1) {
                break;
            }
            else {
                newIn.add(maxPriRequest);
                tmp.remove(maxPriRequest);
            }
        }
        for (PersonRequest personRequest : personIn) {
            if (!newIn.contains(personRequest)) {
                personCnt--;
                print("OUT", floor, id, personRequest.getPersonId(), false);
                Person person = new Person(personRequest, floor.clone());
                requestQueue.addRequest(person);
                notifyAll();
            }
        }
        for (PersonRequest personRequest : newIn) {
            if (!personIn.contains(personRequest)) {
                Floor toFloor = new Floor(personRequest.getToFloor());
                personDirection = floor.direction(toFloor);
                print("IN", floor, id, personRequest.getPersonId(), false);
            }
            personRequests.get(floor).remove(personRequest);
        }
        personIn = newIn;
        updateMFloor();
    }

    public synchronized void setEnd() {
        isEnd = true;
        notifyAll();
    }

    public synchronized boolean isEnd() {
        notifyAll();
        return isEnd;
    }

    public synchronized boolean isEmpty() {
        notifyAll();
        return personCnt == 0;
    }

    public synchronized boolean isArrive(Floor floor) {
        for (PersonRequest personRequest : personIn) {
            if (floor.equals(new Floor(personRequest.getToFloor()))) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isFull(int size) {
        notifyAll();
        return size == personIn.size();
    }

    public synchronized boolean hopeIn(int direction, Floor floor, int size) {
        return judge.hopeIn(direction, floor, size, personRequests);
    }

    public synchronized int countSameDirection(int direction, Floor floor) {
        return judge.countSameDirection(direction, floor, personRequests);
    }

    public synchronized int dir(Floor floor) {
        updateMFloor();
        if (targetFloor != null && floor.equals(targetFloor)) {
            return arrDirection;
        }
        return judge.dir(floor, personCnt, personDirection, minFloor, maxFloor);
    }

    public synchronized int dirOfEmpty(Floor floor) {
        return judge.dirOfEmpty(floor, personRequests);
    }

    public synchronized void setForceDirection(int forceDirection) {
        this.forceDirection = forceDirection;
    }

    public synchronized int getForceDirection() {
        return forceDirection;
    }

    public synchronized int getPersonCount() {
        return personCnt;
    }

    public synchronized boolean havePersonIn() {
        notifyAll();
        return !personIn.isEmpty();
    }

    public synchronized int getPersonDirection() {
        notifyAll();
        return personDirection;
    }
}
