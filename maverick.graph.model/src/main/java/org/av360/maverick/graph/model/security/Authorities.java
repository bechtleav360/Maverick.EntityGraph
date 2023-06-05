package org.av360.maverick.graph.model.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Authorities {

    public static final WeightedAuthority GUEST =  new WeightedAuthority(100, "GUEST");
    public static final WeightedAuthority READER = new WeightedAuthority(200, "READER");


    public static final WeightedAuthority CONTRIBUTOR = new WeightedAuthority(400, "CONTRIBUTOR");
    public static final WeightedAuthority APPLICATION = new WeightedAuthority(600, "APPLICATION");
    public static final WeightedAuthority SYSTEM = new WeightedAuthority(800, "SYSTEM");
    public static List<WeightedAuthority> NO_AUTHORITIES = Collections.emptyList();

    /**
     * Checks if the granted authorities satisfy the required authority. Authorities are inclusive and transitive
     * SYSTEM > APPLICATION > CONTRIBUTOR > READER
     *
     * @param requiredAuthority
     * @param grantedAuthorities
     * @return
     */
    public static boolean satisfies(WeightedAuthority requiredAuthority, Collection<? extends GrantedAuthority> grantedAuthorities) {
        return grantedAuthorities.stream().anyMatch(granted -> granted instanceof WeightedAuthority && ((WeightedAuthority) granted).getInfluence() >= requiredAuthority.getInfluence());
    }

    public static boolean satisfies(GrantedAuthority requiredAuthority, Collection<? extends GrantedAuthority> authorities) {
        Assert.isTrue(requiredAuthority instanceof WeightedAuthority, "Incompatible authority types detected.");
        return satisfies((WeightedAuthority) requiredAuthority, authorities);
    }


    public static class WeightedAuthority implements GrantedAuthority {
        private final int influence;
        private final String label;

        public WeightedAuthority(int influence, String label) {
            this.label = label;
            Assert.isTrue(influence > 0, "A positive number for authorization is required");
            this.influence = influence;
        }

        public String getAuthority() {
            return this.label;
        }

        public int getInfluence() {
            return this.influence;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof WeightedAuthority) {
                return this.influence == ((WeightedAuthority) obj).getInfluence();
            } else if (obj instanceof GrantedAuthority) {
                return this.label.equals(((GrantedAuthority) obj).getAuthority());
            } else return false;
        }

        public int hashCode() {
            return this.label.hashCode();
        }

        public String toString() {
            return this.label;
        }
    }


}
