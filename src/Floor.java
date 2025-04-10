public class Floor {
    private int floor;

    public Floor(String string) {
        if (string.charAt(0) == 'F') {
            this.floor = Integer.parseInt(string.substring(1));
        }
        else {
            this.floor = 1 - Integer.parseInt(string.substring(1));
        }
    }

    public boolean midOf(Floor floor1, Floor floor2) {
        return (floor1.floor < this.floor && floor2.floor > this.floor)
                || (floor1.floor > this.floor && floor2.floor < this.floor);
    }

    public void up() {
        floor++;
    }

    public void down() {
        floor--;
    }

    public boolean goUp(Floor floor1) { // floor1 > floor
        return floor1.floor > this.floor;
    }

    public int direction(Floor floor) {
        if (goUp(floor)) {
            return 1;
        }
        if (equals(floor)) {
            return 0;
        }
        return -1;
    }

    public Floor upFloor() {
        Floor ans = this.clone();
        ans.up();
        return ans;
    }

    public Floor downFloor() {
        Floor ans = this.clone();
        ans.down();
        return ans;
    }

    public int minus(Floor floor1) {
        return this.floor - floor1.floor;
    }

    public String toString() {
        if (floor > 0) {
            return "F" + Integer.toString(floor);
        }
        else {
            return "B" + Integer.toString(1 - floor);
        }
    }

    public Floor clone() {
        return new Floor(this.toString());
    }

    @Override
    public boolean equals(Object obj) {
        Floor floor1 = (Floor) obj;
        return this.floor == floor1.floor;
    }

    @Override
    public int hashCode() {
        return floor;
    }
}
