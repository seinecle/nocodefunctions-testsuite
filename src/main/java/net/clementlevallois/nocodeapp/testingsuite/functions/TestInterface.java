/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.functions;

import java.util.List;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author LEVALLOIS
 */
public interface TestInterface {

    public String getName();
    public void conductTests(List<WebDriver> webDrivers);

    
}
