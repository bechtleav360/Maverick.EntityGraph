package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

public class NoFeatureDependency {

    @Test
    public void some_architecture_rule() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("org.av360.maverick.graph");

        // ArchRule rule = classes().that().resideInAPackage("feature").should()

        //rule.check(importedClasses);
    }
}
