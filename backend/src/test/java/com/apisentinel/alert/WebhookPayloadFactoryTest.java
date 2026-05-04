package com.apisentinel.alert;

import com.apisentinel.config.AppProperties;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorHttpMethod;
import com.apisentinel.organization.Organization;
import com.apisentinel.project.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookPayloadFactoryTest {

    @Test
    void buildsWebhookPayloadWithoutLeakingUrl() throws Exception {
        Organization organization = new Organization("Acme");
        Project project = new Project(organization, "Production APIs", "production");
        Monitor monitor = new Monitor(project, "Billing API", "https://internal.example.com/health", MonitorHttpMethod.GET, 200, 5, 60, 500, 3, "{}", null);
        setId(project, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        setId(monitor, UUID.fromString("00000000-0000-0000-0000-000000000002"));

        WebhookPayloadFactory factory = new WebhookPayloadFactory(new AppProperties(
                "http://localhost:5173",
                new AppProperties.Cors(java.util.List.of("http://localhost:5173")),
                new AppProperties.Jwt("secret", 60),
                null
        ));

        Map<String, Object> payload = factory.build(monitor, null, AlertCondition.MONITOR_DOWN, 123);

        assertThat(payload).containsEntry("event", "MONITOR_DOWN");
        assertThat(payload.toString()).doesNotContain("internal.example.com");
        assertThat(payload.get("dashboardUrl")).isEqualTo("http://localhost:5173/monitors/00000000-0000-0000-0000-000000000002");
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
