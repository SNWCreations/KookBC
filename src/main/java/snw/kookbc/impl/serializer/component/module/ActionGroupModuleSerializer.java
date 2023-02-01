package snw.kookbc.impl.serializer.component.module;

import com.google.gson.*;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.InteractElement;
import snw.jkook.message.component.card.module.ActionGroupModule;
import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 2023/2/1<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class ActionGroupModuleSerializer implements JsonSerializer<ActionGroupModule> {
    @Override
    public JsonElement serialize(ActionGroupModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();

        JsonArray elements = new JsonArray();
        for (InteractElement actionModule : module.getButtons()) {
            // I think the following line maybe throw an exception in the future.
            Validate.isTrue(actionModule instanceof ButtonElement, "If this has error, please tell the author of " + SharedConstants.SPEC_NAME + "! Maybe Kook updated the action module?");
            ButtonElement button = ((ButtonElement) actionModule);
            JsonObject rawButton = new JsonObject();
            rawButton.addProperty("type", "button");
            rawButton.addProperty("theme", button.getTheme().getValue());
            rawButton.addProperty("value", button.getValue());
            if (button.getEventType() == ButtonElement.EventType.LINK) {
                try {
                    new URL(button.getValue());
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Invalid URL for the button", e);
                }
            }
            rawButton.addProperty("click", button.getEventType().getValue());
            rawButton.addProperty("value", button.getValue());
            BaseElement textModule = button.getText();
            JsonElement rawText = context.serialize(textModule);
            rawButton.add("text", rawText);
            elements.add(rawButton);
        }
        moduleObj.addProperty("type", "action-group");
        moduleObj.add("elements", elements);
        return moduleObj;
    }
}
