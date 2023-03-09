module edu.hm.hafner.metric {
    requires java.xml;
    requires org.apache.commons.lang3;
    requires com.github.spotbugs.annotations;
    requires com.google.errorprone.annotations;
    requires edu.hm.hafner.codingstyle;

    exports edu.hm.hafner.coverage;
    exports edu.hm.hafner.coverage.registry;
}
