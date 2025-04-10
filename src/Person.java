import com.oocourse.elevator3.PersonRequest;

public class Person {
    private final PersonRequest request;
    private Floor floorNow;

    public Person(PersonRequest personRequest, Floor floorNow) {
        this.request = personRequest;
        this.floorNow = floorNow;
    }

    public PersonRequest getRequest() {
        return request;
    }

    public Floor getFloorNow() {
        return floorNow;
    }

    public void arriveFloor(Floor floor) {
        this.floorNow = floor;
    }

    public String getFromFloor() {
        return request.getFromFloor();
    }

    public String getToFloor() {
        return request.getToFloor();
    }

    public int getPersonId() {
        return request.getPersonId();
    }

    public int getPriority() {
        return request.getPriority();
    }
}
