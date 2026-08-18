package ui;

import engine.GuessMarketEngine;
import engine.IGuessMarketEngine;
import engine.io.impl.XmlEventsFileLoader;

public class Main {

    public static void main(String[] args) {
        IGuessMarketEngine engine = new GuessMarketEngine(new XmlEventsFileLoader());
        new ConsoleApp(engine).run();
    }
}
