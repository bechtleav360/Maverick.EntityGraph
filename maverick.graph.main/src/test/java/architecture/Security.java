package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.av360.maverick.graph.model.aspects.RequiresPrivilege;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

public class Security {

    @Test
    @Disabled
    public void allPublicServiceMethodsAreAuthorized() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("org.av360.maverick.graph");
        ArchRule rule = ArchRuleDefinition.methods()
                .that().arePublic()
                .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .should().beAnnotatedWith(RequiresPrivilege.class);
        rule.check(importedClasses);
        // ArchRuleDefinition.classes().that().areAnnotatedWith(Service.class).should().
    }
}
