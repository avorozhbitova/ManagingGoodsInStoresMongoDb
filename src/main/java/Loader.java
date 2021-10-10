import mongo.MongoService;

import java.util.Scanner;

public class Loader {
    public static final String COMMANDS = "Доступные команды: \n" +
            "ДОБАВИТЬ_МАГАЗИН - чтобы добавить новый магазин\n" +
            "ДОБАВИТЬ_ТОВАР - чтобы добавить новый товар\n" +
            "ВЫСТАВИТЬ_ТОВАР - чтобы добавить товар в магазин\n" +
            "СТАТИСТИКА_ТОВАРОВ - чтобы вывести информацию по товарам для каждого магазина.";

    private final Scanner scanner = new Scanner(System.in);
    private final MongoService mongoService = new MongoService();
    private boolean stop;

    public void runProgram() {
        System.out.println("Введите одну из команд:");
        System.out.println(COMMANDS);
        System.out.println("Для выхода введите СТОП");
        while (!stop) {
            String input = scanner.nextLine();
            analyzeInput(input);
        }
    }

    private void analyzeInput(String input) {
        String[] fragments = input.split("\\s");
        if (input.startsWith("ДОБАВИТЬ_МАГАЗИН")) {
            addStore(fragments);
        } else if (input.startsWith("ДОБАВИТЬ_ТОВАР")) {
            addProduct(fragments);
        } else if (input.startsWith("ВЫСТАВИТЬ_ТОВАР")) {
            addProductIntoStore(fragments);
        } else if (input.startsWith("СТАТИСТИКА_ТОВАРОВ")) {
            getStatistics();
        } else if (input.equals("СТОП")) {
            stop = true;
        } else {
            System.out.println("Неизвестная команда. " +
                    "Проверьте, нет ли ошибки в запросе.");
            System.out.println(COMMANDS);
        }
    }

    private void addStore(String[] fragments) {
        String storeName = fragments[1];
        mongoService.addStore(storeName);
    }

    private void addProduct(String[] fragments) {
        String productName = fragments[1];
        try {
            int price = Integer.parseInt(fragments[2]);
            mongoService.addProduct(productName, price);
        } catch (NumberFormatException ex) {
            System.out.println("Неверный ввод");
        }
    }

    private void addProductIntoStore(String[] fragments) {
        String productName = fragments[1];
        String storeName = fragments[2];
        mongoService.addProductIntoStore(productName, storeName);
    }

    public void getStatistics() {
        mongoService.getStatistics();
    }

    public void shutdown() {
        mongoService.shutdown();
    }
}
