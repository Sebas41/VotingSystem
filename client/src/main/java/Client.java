import controller.ControllerVoteUI;
import tool.BulkVoteSender;
public class Client {

    private static ControllerVoteUI controller;

    public static void main(String[] args) throws Exception {

        BulkVoteSender.runTest();
    }
}