package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

public class CorrectPackages {

    @Test
    public void correctPackageAssignements() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("org.av360.maverick.graph");

        ArchRule r1 = ArchRuleDefinition.classes().that().areAnnotatedWith(Configuration.class).and().areNotAnnotatedWith(ComponentScan.class).should().resideInAPackage("..config..");

        r1.check(importedClasses);


        ArchRule r2 = ArchRuleDefinition.classes().that().areAnnotatedWith(Service.class).should().resideInAPackage("..services..");
        r2.check(importedClasses);
    }
}
