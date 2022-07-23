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
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.*;
import snw.kookbc.util.Validate;

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
                    if (rawAccessory != null) {
                        Accessory.Mode mode = Accessory.Mode.value(moduleObj.get("mode").getAsString());
                        Validate.notNull(mode, "Unknown accessory mode.");
                        switch (rawAccessory.get("type").getAsString()) {
                            case "image":
                                String src = rawAccessory.get("src").getAsString();
                                accessory = new ImageModule(src, mode);
                                break;
                            case "button":
                                PlainTextModule buttonText;
                                Theme buttonTheme = Theme.value(rawAccessory.get("theme").getAsString());
                                JsonObject rawButtonText = rawAccessory.getAsJsonObject("text");
                                String buttonContent = rawButtonText.get("content").getAsString();
                                switch (rawButtonText.get("type").getAsString()) {
                                    case "plain-text":
                                        buttonText = new PlainTextModule(buttonContent, null);
                                        break;
                                    case "kmarkdown":
                                        buttonText = new MarkdownModule(buttonContent, null);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown button text type.");
                                }
                                accessory = new ButtonModule(ButtonModule.EventType.NO_ACTION, buttonTheme, null, buttonText, mode);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown accessory type.");
                        }
                    }
                    switch (textObject.get("type").getAsString()) {
                        case "plain-text":
                            moduleList.add(new PlainTextModule(textObject.get("content").getAsString(), accessory));
                            break;
                        case "kmarkdown":
                            moduleList.add(new MarkdownModule(textObject.get("content").getAsString(), accessory));
                            break;
                        case "paragraph":
                            int cols = textObject.get("cols").getAsInt();
                            JsonArray fields = textObject.getAsJsonArray("fields");
                            List<PlainTextModule> list = new LinkedList<>();
                            for (JsonElement rawField : fields) {
                                JsonObject fieldObj = rawField.getAsJsonObject();
                                String content = fieldObj.get("content").getAsString();
                                switch (fieldObj.get("type").getAsString()) {
                                    case "plain-text":
                                        list.add(new PlainTextModule(content, null));
                                        break;
                                    case "kmarkdown":
                                        list.add(new MarkdownModule(content, null));
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown paragraph field type");
                                }
                            }
                            moduleList.add(new ParagraphModule(cols, list, accessory));
                            break;
                    }
                    break;
                case "container":
                    List<ImageModule> lst = new LinkedList<>();
                    JsonArray images = moduleObj.getAsJsonArray("elements");
                    for (JsonElement rawImage : images) {
                        JsonObject rawImageObj = rawImage.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawImageObj.get("type").getAsString(), "image"), "Container only accepts image objects.");
                        String src = rawImageObj.get("src").getAsString();
                        lst.add(new ImageModule(src, null));
                    }
                    moduleList.add(new ContainerModule(lst));
                    break;
                case "image-group":
                    List<ImageModule> list = new LinkedList<>();
                    JsonArray imgArray = moduleObj.getAsJsonArray("elements");
                    for (JsonElement rawImage : imgArray) {
                        JsonObject rawImageObj = rawImage.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawImageObj.get("type").getAsString(), "image"), "Container only accepts image objects.");
                        String src = rawImageObj.get("src").getAsString();
                        list.add(new ImageModule(src, null));
                    }
                    moduleList.add(new ImageGroupModule(list));
                    break;
                case "header":
                    JsonObject rawText = moduleObj.getAsJsonObject("text");
                    Validate.isTrue(Objects.equals(rawText.get("type").getAsString(), "plain-text"), "Header module only accepts plain-text.");
                    moduleList.add(new HeaderModule(rawText.get("content").getAsString()));
                    break;
                case "divider":
                    moduleList.add(DividerModule.INSTANCE);
                    break;
                case "action-group":
                    JsonArray elements = moduleObj.getAsJsonArray("elements");
                    List<ActionModule> actionModules = new LinkedList<>();
                    for (JsonElement jsonElement : elements) {
                        JsonObject rawButton = jsonElement.getAsJsonObject();
                        Validate.isTrue(Objects.equals(rawButton.get("type").getAsString(), "button"), "Action Group module only accepts button.");
                        String value = rawButton.get("value") != null ? rawButton.get("value").getAsString() : "";
                        ButtonModule.EventType type = ButtonModule.EventType.value(rawButton.get("click") != null ? rawButton.get("click").getAsString() : "");
                        PlainTextModule buttonText;
                        Theme buttonTheme = Theme.value(rawButton.get("theme").getAsString());
                        JsonObject rawButtonText = rawButton.getAsJsonObject("text");
                        String buttonContent = rawButtonText.get("content").getAsString();
                        switch (rawButtonText.get("type").getAsString()) {
                            case "plain-text":
                                buttonText = new PlainTextModule(buttonContent, null);
                                break;
                            case "kmarkdown":
                                buttonText = new MarkdownModule(buttonContent, null);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown button text type.");
                        }
                        actionModules.add(new ButtonModule(type, buttonTheme, value, buttonText));
                    }
                    moduleList.add(new ActionGroupModule(actionModules));
                    break;
                case "context":
                    JsonArray contextElements = moduleObj.getAsJsonArray("elements");
                    List<BaseModule> contextModules = new LinkedList<>();
                    for (JsonElement contextElement : contextElements) {
                        JsonObject contextObj = contextElement.getAsJsonObject();
                        switch (contextObj.get("type").getAsString()) {
                            case "image":
                                String src = contextObj.get("src").getAsString();
                                contextModules.add(new ImageModule(src, null));
                                break;
                            case "plain-text":
                                String content = contextObj.get("content").getAsString();
                                contextModules.add(new PlainTextModule(content, null));
                                break;
                            case "kmarkdown":
                                String kmdContent = contextObj.get("content").getAsString();
                                contextModules.add(new MarkdownModule(kmdContent, null));
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
                    int fileSize = moduleObj.get("size").getAsInt();
                    moduleList.add(new FileModule(new FileComponent(src, title, fileSize, FileComponent.Type.FILE)));
                    break;
                case "audio":
                    String audioTitle = moduleObj.get("title").getAsString();
                    String audioSrc = moduleObj.get("src").getAsString();
                    String audioCover = moduleObj.get("cover").getAsString();
                    moduleList.add(new AudioModule(audioTitle, audioSrc, audioCover));
                    break;
                case "video":
                    String videoTitle = moduleObj.get("title").getAsString();
                    String videoSrc = moduleObj.get("src").getAsString();
                    moduleList.add(new VideoModule(videoTitle, videoSrc));
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
            if (module instanceof PlainTextModule) {
                JsonObject textObj = new JsonObject();
                moduleObj.addProperty("type", "section");
                textObj.addProperty("type", (module instanceof MarkdownModule) ? "kmarkdown" : "plain-text");
                textObj.addProperty("content", ((PlainTextModule) module).getValue());
                moduleObj.add("text", textObj);
                addAccessory(((PlainTextModule) module).getAccessory(), moduleObj);
            } else if (module instanceof ParagraphModule) {
                moduleObj.addProperty("type", "section");
                JsonObject textObj = new JsonObject();
                textObj.addProperty("type", "paragraph");
                textObj.addProperty("cols", ((ParagraphModule) module).getColumns());
                JsonArray fields = new JsonArray();
                for (PlainTextModule textModule : ((ParagraphModule) module).getModules()) {
                    String content = textModule.getValue();
                    String type = (textModule instanceof MarkdownModule) ? "kmarkdown" : "plain-text";
                    JsonObject fieldObj = new JsonObject();
                    fieldObj.addProperty("type", type);
                    fieldObj.addProperty("content", content);
                    fields.add(fieldObj);
                }
                textObj.add("fields", fields);
                moduleObj.add("text", textObj);
                addAccessory(((ParagraphModule) module).getAccessory(), moduleObj);
            } else if (module instanceof ContainerModule) {
                JsonArray elements = new JsonArray();
                for (ImageModule image : ((ContainerModule) module).getImages()) {
                    JsonObject element = new JsonObject();
                    element.addProperty("type", "image");
                    element.addProperty("src", image.getSource());
                    elements.add(element);
                }
                moduleObj.addProperty("type", "container");
                moduleObj.add("elements", elements);
            } else if (module instanceof ImageGroupModule) {
                JsonArray elements = new JsonArray();
                for (ImageModule image : ((ImageGroupModule) module).getImages()) {
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
                textObj.addProperty("content", ((HeaderModule) module).getValue());
                moduleObj.addProperty("type", "header");
                moduleObj.add("text", textObj);
            } else if (module instanceof DividerModule) {
                moduleObj.addProperty("type", "divider");
            } else if (module instanceof ActionGroupModule) {
                JsonArray elements = new JsonArray();
                for (ActionModule actionModule : ((ActionGroupModule) module).getButtons()) {
                    // I think the following line maybe throw an exception in the future.
                    Validate.isTrue(actionModule instanceof ButtonModule, "If this has error, please tell the author of KookBC! Maybe Kook updated the action module?");
                    ButtonModule button = ((ButtonModule) actionModule);
                    JsonObject rawButton = new JsonObject();
                    rawButton.addProperty("type", "button");
                    rawButton.addProperty("theme", button.getTheme().getValue());
                    rawButton.addProperty("value", button.getValue());
                    if (button.getType() == ButtonModule.EventType.LINK) {
                        try {
                            new URL(button.getValue());
                        } catch (MalformedURLException e) {
                            throw new RuntimeException("Invalid URL for the button", e);
                        }
                    }
                    rawButton.addProperty("click", button.getType().getValue());
                    rawButton.addProperty("value", button.getValue());
                    PlainTextModule textModule = button.getText();
                    String content = textModule.getValue();
                    String type = (textModule instanceof MarkdownModule) ? "kmarkdown" : "plain-text";
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
                for (BaseModule base : ((ContextModule) module).getModules()) {
                    JsonObject rawObj = new JsonObject();
                    if (base instanceof PlainTextModule) {
                        PlainTextModule textModule = (PlainTextModule) base;
                        String content = textModule.getValue();
                        String type = (textModule instanceof MarkdownModule) ? "kmarkdown" : "plain-text";
                        rawObj.addProperty("type", type);
                        rawObj.addProperty("content", content);
                        elements.add(rawObj);
                    } else if (base instanceof ImageModule) {
                        ImageModule image = (ImageModule) base;
                        rawObj.addProperty("type", "image");
                        rawObj.addProperty("src", image.getSource());
                        elements.add(rawObj);
                    }
                }
                moduleObj.addProperty("type", "context");
                moduleObj.add("elements", elements);
            } else if (module instanceof FileModule) {
                FileComponent file = ((FileModule) module).getComponent();
                moduleObj.addProperty("type", "file");
                moduleObj.addProperty("title", (file.getTitle()));
                moduleObj.addProperty("src", file.getUrl());
                moduleObj.addProperty("size", file.getSize());
            } else if (module instanceof AudioModule) {
                moduleObj.addProperty("type", "audio");
                moduleObj.addProperty("title", ((AudioModule) module).getTitle());
                moduleObj.addProperty("src", ((AudioModule) module).getSource());
                moduleObj.addProperty("cover", ((AudioModule) module).getCover());
            } else if (module instanceof VideoModule) {
                moduleObj.addProperty("type", "video");
                moduleObj.addProperty("title", ((VideoModule) module).getTitle());
                moduleObj.addProperty("src", ((VideoModule) module).getSource());
            } else if (module instanceof CountdownModule) {
                moduleObj.addProperty("type", "countdown");
                moduleObj.addProperty("mode", ((CountdownModule) module).getType().getValue());
                moduleObj.addProperty("endTime", ((CountdownModule) module).getEndTime());
            } else if (module instanceof ButtonModule) { // special support for single ButtonModule
                JsonArray elements = new JsonArray();
                ButtonModule button = ((ButtonModule) module);
                JsonObject rawButton = new JsonObject();
                rawButton.addProperty("type", "button");
                rawButton.addProperty("theme", button.getTheme().getValue());
                rawButton.addProperty("value", button.getValue());
                if (button.getType() == ButtonModule.EventType.LINK) {
                    try {
                        new URL(button.getValue());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Invalid URL for the button", e);
                    }
                }
                rawButton.addProperty("click", button.getType().getValue());
                rawButton.addProperty("value", button.getValue());
                PlainTextModule textModule = button.getText();
                String content = textModule.getValue();
                String type = (textModule instanceof MarkdownModule) ? "kmarkdown" : "plain-text";
                JsonObject rawText = new JsonObject();
                rawText.addProperty("type", type);
                rawText.addProperty("content", content);
                rawButton.add("text", rawText);
                elements.add(rawButton);
                moduleObj.addProperty("type", "action-group");
                moduleObj.add("elements", elements);
            }
            modules.add(moduleObj);
        }
        object.add("modules", modules);
        return object;
    }

    private static void addAccessory(Accessory accessory, JsonObject moduleObj) {
        if (accessory == null) return;
        JsonObject accessoryJson = new JsonObject();
        Accessory.Mode mode = accessory.getMode();
        if (accessory instanceof ImageModule) {
            accessoryJson.addProperty("type", "image");
            accessoryJson.addProperty("src", ((ImageModule) accessory).getSource());
            accessoryJson.addProperty("size", "lg");
        } else if (accessory instanceof ButtonModule) {
            accessoryJson.addProperty("type", "button");
            accessoryJson.addProperty("theme", ((ButtonModule) accessory).getTheme().getValue());
            JsonObject textObj = new JsonObject();
            PlainTextModule textModule = ((ButtonModule) accessory).getText();
            textObj.addProperty("type", (textModule instanceof MarkdownModule) ? "kmarkdown" : "plain-text");
            textObj.addProperty("content", textModule.getValue());
            accessoryJson.add("text", textObj);
        }
        moduleObj.addProperty("mode", mode.getValue());
        moduleObj.add("accessory", accessoryJson);
    }
}
