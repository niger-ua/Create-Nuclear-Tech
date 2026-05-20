package cattodream.createnucleartech.radiation;

public record ContainmentResult(
        ContainmentStatus status,
        int volumeRadius,
        double leakFactor,
        int leadFaces,
        int shieldedFaces
) {
    public boolean fullyContained() {
        return status == ContainmentStatus.BLOCKED || status == ContainmentStatus.CONTAINED;
    }
}
