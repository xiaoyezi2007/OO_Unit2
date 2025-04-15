import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.UpdateRequest;

import java.util.ArrayList;

public class RequestQueue {
    private final ArrayList<Person> requests;
    private ArrayList<ScheRequest> scheRequests;
    private ArrayList<UpdateRequest> updateRequests;
    private boolean isEnd;

    public RequestQueue() {
        requests = new ArrayList<>();
        scheRequests = new ArrayList<>();
        updateRequests = new ArrayList<>();
        isEnd = false;
    }

    public synchronized void addRequest(Person person) {
        requests.add(person);
        notifyAll();
    }

    public synchronized void addScheRequest(ScheRequest scheRequest) {
        scheRequests.add(scheRequest);
        notifyAll();
    }

    public synchronized void addUpdateRequest(UpdateRequest updateRequest) {
        updateRequests.add(updateRequest);
        notifyAll();
    }

    public synchronized Person poll() {
        if (!scheRequests.isEmpty()) {
            return null;
        }
        if (requests.isEmpty()) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (requests.isEmpty()) {
            return null;
        }
        notifyAll();
        return requests.remove(0);
    }

    public synchronized ScheRequest pollScheRequest() {
        if (scheRequests.isEmpty()) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (scheRequests.isEmpty()) {
            return null;
        }
        notifyAll();
        return scheRequests.remove(0);
    }

    public synchronized UpdateRequest pollUpdateRequest() {
        if (updateRequests.isEmpty()) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (updateRequests.isEmpty()) {
            return null;
        }
        notifyAll();
        return updateRequests.remove(0);
    }

    public synchronized void setEnd() {
        isEnd = true;
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        notifyAll();
        return requests.isEmpty() && scheRequests.isEmpty() && updateRequests.isEmpty();
    }

    public synchronized boolean isEnd() {
        notifyAll();
        return isEnd;
    }

    public synchronized boolean scheEmpty() {
        notifyAll();
        return scheRequests.isEmpty();
    }

    public synchronized boolean updateEmpty() {
        notifyAll();
        return updateRequests.isEmpty();
    }
}
