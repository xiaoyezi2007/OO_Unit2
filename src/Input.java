import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.UpdateRequest;

import java.io.IOException;

public class Input implements Runnable {
    private RequestQueue requestQueue;

    public Input(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public void run() {
        ElevatorInput elevatorInput = new ElevatorInput(System.in);
        while (true) {
            Request request = elevatorInput.nextRequest();
            if (request == null) {
                requestQueue.setEnd();
                break;
            } else {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    Floor fromFloor = new Floor(personRequest.getFromFloor());
                    Person person = new Person(personRequest, fromFloor);
                    requestQueue.addRequest(person);
                }
                else if (request instanceof ScheRequest) {
                    ScheRequest scheRequest = (ScheRequest) request;
                    requestQueue.addScheRequest(scheRequest);
                }
                else if (request instanceof UpdateRequest) {
                    UpdateRequest updateRequest = (UpdateRequest) request;
                    requestQueue.addUpdateRequest(updateRequest);
                }
            }
        }
        try {
            elevatorInput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
