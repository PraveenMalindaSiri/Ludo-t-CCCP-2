package mystery;

public final class MysteryOutcome {
    public enum Type {
        ALPHA_ENERGIZED,
        ALPHA_SICK,
        BETA,
        GAMMA_DIRECTION_CHANGED,
        GAMMA_TO_BETA,
        BASE,
        START,
        APPROACH
    }

    private final Type type;
    private final String destination;
    private final String oldDirection;
    private final String newDirection;

    public MysteryOutcome(Type type, String destination) {
        this(type, destination, null, null);
    }

    public MysteryOutcome(Type type, String destination,
                          String oldDirection, String newDirection) {
        this.type = type;
        this.destination = destination;
        this.oldDirection = oldDirection;
        this.newDirection = newDirection;
    }

    public Type getType() {
        return type;
    }

    public String getDestination() {
        return destination;
    }

    public String getOldDirection() {
        return oldDirection;
    }

    public String getNewDirection() {
        return newDirection;
    }
}
