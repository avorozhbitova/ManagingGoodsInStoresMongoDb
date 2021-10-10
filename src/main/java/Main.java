public class Main {
    public static void main(String[] args) {
        Loader loader = new Loader();
        loader.runProgram();

        loader.getStatistics();

        loader.shutdown();
    }
}
