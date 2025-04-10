import com.oocourse.elevator3.TimableOutput;

import java.util.HashMap;

public class MainClass {
    public static void main(String[] args) throws Exception {
        TimableOutput.initStartTimestamp();

        RequestQueue requestQueue = new RequestQueue();
        Thread input = new Thread(new Input(requestQueue));
        HashMap<Integer, ElevatorQueue> queueMap = new HashMap<>();
        Thread dispatch = new Thread(new Dispatch(queueMap, requestQueue));
        for (int i = 1; i <= 6; i++) {
            ElevatorQueue elevatorQueue = new ElevatorQueue(i, requestQueue);
            Thread elevator = new Thread(new Elevator(i, elevatorQueue));
            queueMap.put(i, elevatorQueue);
            elevator.start();
        }
        input.start();
        dispatch.start();
    }
}