/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.entity.builder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.card.*;
import snw.jkook.message.component.card.element.*;
import snw.jkook.message.component.card.module.*;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.jkook.util.Validate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Just for (de)serialize the CardMessages.
public class CardBuilder {

    public static MultipleCardComponent buildCard(JsonArray array) {
        List<CardComponent> components = new LinkedList<>();
        for (JsonElement jsonElement : array) {
            components.add(buildCard(jsonElement.getAsJsonObject()));
        }
        return new MultipleCardComponent(components);
    }

    public static CardComponent buildCard(JsonObject object) {
        Validate.isTrue(Objects.equals(object.get("type").getAsString(), "card"), "The provided element is not a card.");
        Theme theme = Theme.value(object.get("theme").getAsString());
        Size size = Size.value(object.get("size").getAsString());

        List<BaseModule> moduleList = new LinkedList<>();
        JsonArray modules = object.getAsJsonArray("modules");
        for (JsonElement element : modules) {
            JsonObject moduleObj = element.getAsJsonObject();
            switch (moduleObj.get("type").getAsString()) {
                case "section":
                    JsonObject textObject = moduleObj.getAsJsonObject("text");
                    JsonObject rawAccessory = moduleObj.getAsJsonObject("accessory");
                    Accessory accessory = null;
                    Accessory.Mode mode = null;
                    if (rawAccessory != null) {
                        mode = Accessory.Mode.value(moduleObj.get("mode").getAsString());
                        // Validate.notNull(mode, "Unknown accessory mode."); // accessories without mode is allowed in KOOK CardMessage builder.
                        switch (rawAccessory.get("type").getAsString()) {
                            case "image":
                                String src = rawAccessory.get("src").getAsString();
                                accessory = new ImageElement(src, null, false);
                                break;
                            case "button":
                                BaseElement buttonText;
                                Theme buttonTheme = Theme.value(rawAccessory.get("theme").getAsString());
                                JsonObject rawButtonText = rawAccessory.getAsJsonObject("text");
                                String buttonContent = rawButtonText.get("content").getAsString();
                                switch (rawButtonText.get("type").getAsString()) {
                                    case "plain-text":
                                        buttonText = new PlainTextElement(buttonContent, false);
                                        break;
                                    case "kmarkdown":
                                        buttonText = new MarkdownElement(buttonContent);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown button text type.");
                                }
                                accessory = new ButtonElement(buttonTheme, rawAccessory.has("value") ? rawAccessory.get("value").getAsString() : null, ButtonElement.EventType.NO_ACTION, buttonText);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown accessory type.");
                        }
                    }
                    CardScopeElement text;
                    switch (textObject.get("type").getAsString()) {
                        case "plain-text":
                            text = new PlainTextElement(textObject.get("content").getAsString(), false);
                            break;
                        case "kmarkdown":
                            text = new MarkdownElement(textObject.get("content").getAsString());
                            break;
                        case "paragraph":
                            int cols = textObject.get("cols").getAsInt();
                            JsonArray fields = textObject.getAsJsonArray("fields");
                            List<BaseElement> list = new LinkedList<>();
                            for (JsonElement rawField : fields) {
                                JsonObject fieldObj = rawField.getAsJsonObject();
                                String content = fieldObj.get("content").getAsString();
                                switch (fieldObj.get("type").getAsString()) {
                                    case "plain-text":
                                        list.add(new PlainTextElement(content, false));
                                        break;
                                    case "kmarkdown":
                                        list.add(new MarkdownElement(content));
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown paragraph field type");
                                }
                            }
                            text = new Paragraph(cols, list);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown type of the element in SectionModule.");
                    }
                    moduleList.add(new SectionModule(text, accessory, mode));
                    break;
                case "container":
                    List<ImageElement> lst = new LinkedList<>();
                    JsonArray images = moduleObj.getAsJsonArray("elements");
                    for (JsonElement rawImage : images) {
                        JsonObject rawImageObj = rawImage.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawImageObj.get("type").getAsString(), "image"), "Container only accepts image objects.");
                        String src = rawImageObj.get("src").getAsString();
                        lst.add(new ImageElement(src, null, false));
                    }
                    moduleList.add(new ContainerModule(lst));
                    break;
                case "image-group":
                    List<ImageElement> list = new LinkedList<>();
                    JsonArray imgs = moduleObj.getAsJsonArray("elements");
                    for (JsonElement rawImage : imgs) {
                        JsonObject rawImageObj = rawImage.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawImageObj.get("type").getAsString(), "image"), "Image group only accepts image objects.");
                        String src = rawImageObj.get("src").getAsString();
                        list.add(new ImageElement(src, null, false));
                    }
                    moduleList.add(new ImageGroupModule(list));
                    break;
                case "header":
                    JsonObject rawText = moduleObj.getAsJsonObject("text");
                    Validate.isTrue(Objects.equals(rawText.get("type").getAsString(), "plain-text"), "Header module only accepts plain-text.");
                    moduleList.add(new HeaderModule(new PlainTextElement(rawText.get("content").getAsString(), false)));
                    break;
                case "divider":
                    moduleList.add(DividerModule.INSTANCE);
                    break;
                case "action-group":
                    JsonArray elements = moduleObj.getAsJsonArray("elements");
                    List<InteractElement> actionModules = new LinkedList<>();
                    for (JsonElement jsonElement : elements) {
                        JsonObject rawButton = jsonElement.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawButton.get("type").getAsString(), "button"), "Action Group module only accepts button.");
                        String value = rawButton.get("value") != null ? rawButton.get("value").getAsString() : "";
                        ButtonElement.EventType type = ButtonElement.EventType.value(rawButton.get("click") != null ? rawButton.get("click").getAsString() : "");
                        BaseElement buttonText;
                        Theme buttonTheme = Theme.value(rawButton.get("theme").getAsString());
                        JsonObject rawButtonText = rawButton.getAsJsonObject("text");
                        String buttonContent = rawButtonText.get("content").getAsString();
                        switch (rawButtonText.get("type").getAsString()) {
                            case "plain-text":
                                buttonText = new PlainTextElement(buttonContent, false);
                                break;
                            case "kmarkdown":
                                buttonText = new MarkdownElement(buttonContent);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown button text type.");
                        }
                        actionModules.add(new ButtonElement(buttonTheme, value, type, buttonText));
                    }
                    moduleList.add(new ActionGroupModule(actionModules));
                    break;
                case "context":
                    JsonArray contextElements = moduleObj.getAsJsonArray("elements");
                    List<BaseElement> contextModules = new LinkedList<>();
                    for (JsonElement contextElement : contextElements) {
                        JsonObject contextObj = contextElement.getAsJsonObject();
                        switch (contextObj.get("type").getAsString()) {
                            case "image":
                                String src = contextObj.get("src").getAsString();
                                contextModules.add(new ImageElement(src, null, false));
                                break;
                            case "plain-text":
                                String content = contextObj.get("content").getAsString();
                                contextModules.add(new PlainTextElement(content, false));
                                break;
                            case "kmarkdown":
                                String kmdContent = contextObj.get("content").getAsString();
                                contextModules.add(new MarkdownElement(kmdContent));
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown context type.");
                        }
                    }
                    moduleList.add(new ContextModule(contextModules));
                    break;
                case "file":
                    String title = moduleObj.get("title").getAsString();
                    String src = moduleObj.get("src").getAsString();
                    moduleList.add(new FileModule(FileComponent.Type.FILE, src, title, null));
                    break;
                case "audio":
                    title = moduleObj.get("title").getAsString();
                    src = moduleObj.get("src").getAsString();
                    String cover = moduleObj.get("cover").getAsString();
                    moduleList.add(new FileModule(FileComponent.Type.AUDIO, src, title, cover));
                    break;
                case "video":
                    title = moduleObj.get("title").getAsString();
                    src = moduleObj.get("src").getAsString();
                    moduleList.add(new FileModule(FileComponent.Type.VIDEO, src, title, null));
                    break;
                case "countdown":
                    CountdownModule.Type type = CountdownModule.Type.value(moduleObj.get("mode").getAsString());
                    long endTime = moduleObj.get("endTime").getAsLong();
                    moduleList.add(new CountdownModule(type, endTime));
                    break;
            }
        }
        return new CardComponent(moduleList, size, theme);
    }

    public static JsonArray serialize(CardComponent component) {
        JsonArray result = new JsonArray();
        result.add(serialize0(component));
        return result;
    }

    public static String serialize(MultipleCardComponent component) {
        JsonArray array = new JsonArray();
        for (CardComponent card : component.getComponents()) {
            array.add(serialize0(card));
        }
        return new Gson().toJson(array);
    }

    public static JsonObject serialize0(CardComponent component) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "card");
        object.addProperty("theme", component.getTheme().getValue());
        object.addProperty("size", component.getSize().getValue());
        JsonArray modules = new JsonArray();
        for (BaseModule module : component.getModules()) {
            JsonObject moduleObj = new JsonObject();
            if (module instanceof SectionModule) {
                SectionModule sectionModule = (SectionModule) module;
                CardScopeElement text = sectionModule.getText();
                JsonObject rawText = new JsonObject();
                if (text instanceof PlainTextElement) {
                    rawText.addProperty("type", "plain-text");
                    rawText.addProperty("content", ((PlainTextElement) text).getContent());
                } else if (text instanceof MarkdownElement) {
                    rawText.addProperty("type", "kmarkdown");
                    rawText.addProperty("content", ((MarkdownElement) text).getContent());
                } else if (text instanceof Paragraph) {
                    rawText.addProperty("type", "paragraph");
                    rawText.addProperty("cols", ((Paragraph) text).getColumns());
                    JsonArray elements = new JsonArray();
                    for (BaseElement field : ((Paragraph) text).getFields()) {
                        JsonObject rawElement = new JsonObject();
                        if (field instanceof PlainTextElement) {
                            rawElement.addProperty("type", "plain-text");
                            rawElement.addProperty("content", ((PlainTextElement) field).getContent());
                        } else if (field instanceof MarkdownElement) {
                            rawElement.addProperty("type", "kmarkdown");
                            rawElement.addProperty("content", ((MarkdownElement) field).getContent());
                        } else {
                            throw new IllegalArgumentException("Unsupported element type found in Paragraph.");
                        }
                        elements.add(rawElement);
                    }
                    rawText.add("fields", elements);
                } else {
                    throw new IllegalArgumentException("Unsupported element type found in SectionModule.");
                }
                moduleObj.addProperty("type", "section");
                moduleObj.add("text", rawText);
                Accessory.Mode mode = ((SectionModule) module).getMode();
                if (mode != null) {
                    moduleObj.addProperty("mode", mode.getValue());
                }
                addAccessory(sectionModule.getAccessory(), moduleObj);
            } else if (module instanceof ContainerModule) {
                JsonArray elements = new JsonArray();
                for (ImageElement image : ((ContainerModule) module).getImages()) {
                    JsonObject element = new JsonObject();
                    element.addProperty("type", "image");
                    element.addProperty("src", image.getSource());
                    elements.add(element);
                }
                moduleObj.addProperty("type", "container");
                moduleObj.add("elements", elements);
            } else if (module instanceof ImageGroupModule) {
                JsonArray elements = new JsonArray();
                for (ImageElement image : ((ImageGroupModule) module).getImages()) {
                    JsonObject element = new JsonObject();
                    element.addProperty("type", "image");
                    element.addProperty("src", image.getSource());
                    elements.add(element);
                }
                moduleObj.addProperty("type", "image-group");
                moduleObj.add("elements", elements);
            } else if (module instanceof HeaderModule) {
                JsonObject textObj = new JsonObject();
                textObj.addProperty("type", "plain-text");
                textObj.addProperty("content", ((HeaderModule) module).getElement().getContent());
                moduleObj.addProperty("type", "header");
                moduleObj.add("text", textObj);
            } else if (module instanceof DividerModule) {
                moduleObj.addProperty("type", "divider");
            } else if (module instanceof ActionGroupModule) {
                JsonArray elements = new JsonArray();
                for (InteractElement actionModule : ((ActionGroupModule) module).getButtons()) {
                    // I think the following line maybe throw an exception in the future.
                    Validate.isTrue(actionModule instanceof ButtonElement, "If this has error, please tell the author of KookBC! Maybe Kook updated the action module?");
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
                    String type = (textModule instanceof MarkdownElement) ? "kmarkdown" : "plain-text";
                    String content = (textModule instanceof MarkdownElement) ? ((MarkdownElement) textModule).getContent() : ((PlainTextElement) textModule).getContent();
                    JsonObject rawText = new JsonObject();
                    rawText.addProperty("type", type);
                    rawText.addProperty("content", content);
                    rawButton.add("text", rawText);
                    elements.add(rawButton);
                }
                moduleObj.addProperty("type", "action-group");
                moduleObj.add("elements", elements);
            } else if (module instanceof ContextModule) {
                JsonArray elements = new JsonArray();
                for (BaseElement base : ((ContextModule) module).getModules()) {
                    JsonObject rawObj = new JsonObject();
                    if (base instanceof PlainTextElement || base instanceof MarkdownElement) {
                        String content;
                        String type;
                        if (base instanceof PlainTextElement) {
                            type = "plain-text";
                            content = ((PlainTextElement) base).getContent();
                        } else {
                            type = "kmarkdown";
                            content = ((MarkdownElement) base).getContent();
                        }
                        rawObj.addProperty("type", type);
                        rawObj.addProperty("content", content);
                        elements.add(rawObj);
                    } else if (base instanceof ImageElement) {
                        ImageElement image = (ImageElement) base;
                        rawObj.addProperty("type", "image");
                        rawObj.addProperty("src", image.getSource());
                        elements.add(rawObj);
                    }
                }
                moduleObj.addProperty("type", "context");
                moduleObj.add("elements", elements);
            } else if (module instanceof FileModule) {
                FileModule file = ((FileModule) module);
                moduleObj.addProperty("type", file.getType().getValue());
                moduleObj.addProperty("title", (file.getTitle()));
                moduleObj.addProperty("src", file.getSource());
                if (file.getType() == FileComponent.Type.AUDIO) {
                    moduleObj.addProperty("cover", file.getCover());
                }
            } else if (module instanceof CountdownModule) {
                moduleObj.addProperty("type", "countdown");
                moduleObj.addProperty("mode", ((CountdownModule) module).getType().getValue());
                moduleObj.addProperty("endTime", ((CountdownModule) module).getEndTime());
            }
            modules.add(moduleObj);
        }
        object.add("modules", modules);
        return object;
    }

    private static void addAccessory(Accessory accessory, JsonObject moduleObj) {
        if (accessory == null) return;
        JsonObject accessoryJson = new JsonObject();
        Accessory.Mode mode = moduleObj.has("mode") ? Accessory.Mode.value(moduleObj.get("mode").getAsString()) : null;
        if (accessory instanceof ImageElement) {
            accessoryJson.addProperty("type", "image");
            accessoryJson.addProperty("src", ((ImageElement) accessory).getSource());
            accessoryJson.addProperty("size", ((ImageElement) accessory).getSize().getValue());
        } else if (accessory instanceof ButtonElement) {
            accessoryJson.addProperty("type", "button");
            accessoryJson.addProperty("theme", ((ButtonElement) accessory).getTheme().getValue());
            JsonObject textObj = new JsonObject();
            BaseElement textModule = ((ButtonElement) accessory).getText();
            textObj.addProperty("type", (textModule instanceof MarkdownElement) ? "kmarkdown" : "plain-text");
            textObj.addProperty("content",
                    (textModule instanceof MarkdownElement) ?
                            ((MarkdownElement) textModule).getContent() :
                            ((PlainTextElement) textModule).getContent()
            );
            accessoryJson.add("text", textObj);
        }
        if (mode != null) {
            moduleObj.addProperty("mode", mode.getValue());
        }
        moduleObj.add("accessory", accessoryJson);
    }
}
