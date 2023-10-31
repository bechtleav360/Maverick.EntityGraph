package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.av360.maverick.graph.model.aspects.Job;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class CorrectPackages {

    @Test
    public void checkLayers() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Persistence").definedBy("..store..")

                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");
    }

    @Test
    public void correctPackageAssignements() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("org.av360.maverick.graph");

        ArchRule r1 = ArchRuleDefinition.classes().that().areAnnotatedWith(Configuration.class).and().areNotAnnotatedWith(ComponentScan.class).should().resideInAPackage("..config..");

        r1.check(importedClasses);


        ArchRule r2 = ArchRuleDefinition.classes().that().areAnnotatedWith(Service.class).should().resideInAPackage("..services..");
        r2.check(importedClasses);


        ArchRule r3 = ArchRuleDefinition.classes().that().areAnnotatedWith(Job.class).should().resideInAPackage("..jobs..");
        r3.check(importedClasses);
    }

    @Test
    public void serviceShouldOnlyBeAccessedByController() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("org.av360.maverick.graph");

        ArchRule rule = ArchRuleDefinition.classes().that().areAnnotatedWith(Service.class).should().onlyBeAccessed().byAnyPackage("..controller..", "..services..", "..jobs..");
        rule.check(importedClasses);
    }


}
