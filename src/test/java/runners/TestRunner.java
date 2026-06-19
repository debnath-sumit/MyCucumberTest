package runners;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "steps,hooks")
@ConfigurationParameter(
		key = Constants.PLUGIN_PROPERTY_NAME,
		value = "pretty, html:target/cucumber-report.html, json:target/cucumber.json"
)
public class TestRunner {
}