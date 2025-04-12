import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.UpdateRequest;

import java.util.HashMap;

import static java.lang.Thread.sleep;

public class Dispatch implements Runnable {
    private HashMap<Integer, ElevatorQueue> queueMap;
    private RequestQueue requestQueue;

    public Dispatch(HashMap<Integer, ElevatorQueue> queueMap, RequestQueue requestQueue) {
        this.queueMap = queueMap;
        this.requestQueue = requestQueue;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        while (true) {
            if (requestQueue.isEmpty() && requestQueue.isEnd() && this.isEnd()) {
                for (ElevatorQueue queue : queueMap.values()) {
                    queue.setEnd();
                }
                break;
            }
            ScheRequest scheRequest = requestQueue.pollScheRequest();
            if (scheRequest != null) {
                dispatchScheRequest(scheRequest);
            }
            UpdateRequest updateRequest = requestQueue.pollUpdateRequest();
            if (updateRequest != null) {
                dispatchUpdateRequest(updateRequest);
            }
            Person person = requestQueue.poll();
            if (person != null) {
                dispatch(person);
            }
        }
    }

    public void dispatchScheRequest(ScheRequest scheRequest) {
        int id = scheRequest.getElevatorId();
        queueMap.get(id).addScheRequest(scheRequest);
    }

    public void dispatch(Person person) {
        while (true) {
            for (ElevatorQueue queue : queueMap.values()) {
                if (queue.isEmpty() && !queue.isSilence() && queue.takeable(person)) {
                    if (queue.addPersonRequest(person)) {
                        return;
                    }
                }
            }
            for (ElevatorQueue queue : queueMap.values()) {
                if (!queue.isSilence() && queue.arriveable(person) && queue.getPersonCount() < 12) {
                    if (queue.addPersonRequest(person)) {
                        return;
                    }
                }
            }
            for (ElevatorQueue queue : queueMap.values()) {
                if (!queue.isSilence() && !queue.dirIsFull(person)) {
                    if (queue.addPersonRequest(person)) {
                        return;
                    }
                }
            }
            ElevatorQueue bestQueue = queueMap.get(1);
            int minPersonCnt = 10000;
            for (ElevatorQueue queue : queueMap.values()) {
                if (queue.isSilence() || !queue.takeable(person)) {
                    continue;
                }
                if (queue.getPersonCount() < minPersonCnt) {
                    minPersonCnt = queue.getPersonCount();
                    bestQueue = queue;
                }
            }
            if (!bestQueue.isSilence() && bestQueue.getPersonCount() < 12
                && bestQueue.takeable(person)) {
                if (bestQueue.addPersonRequest(person)) {
                    return;
                }
            }
            try {
                sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public synchronized boolean isEnd() {
        boolean end = true;
        for (ElevatorQueue queue : queueMap.values()) {
            if (!queue.isEmpty() || queue.isSilence()) {
                end = false;
            }
        }
        if (end) {
            notifyAll();
            return true;
        }
        try {
            wait(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public synchronized void dispatchUpdateRequest(UpdateRequest updateRequest) {
        int idA = updateRequest.getElevatorAId();
        int idB = updateRequest.getElevatorBId();
        ElevatorQueue queueA = queueMap.get(idA);
        ElevatorQueue queueB = queueMap.get(idB);
        String targetFloor = updateRequest.getTransferFloor();
        SameWell sameWell = new SameWell(queueA, queueB, targetFloor);
        queueA.addUpdateRequest(sameWell);
        queueB.addUpdateRequest(sameWell);
    }
}
