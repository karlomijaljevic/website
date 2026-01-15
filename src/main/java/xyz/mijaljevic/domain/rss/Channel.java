/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */
package xyz.mijaljevic.domain.rss;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * Represents the <i>channel</i> (composite) element in the RSS XML file that
 * the website serves for RSS readers.
 */
@Data
@SuppressWarnings("unused")
@XmlRootElement(name = "channel")
public final class Channel {
    private String title;
    private String link;
    private String description;
    private String language;
    private String lastBuildDate;
    private String webMaster;
    private AtomLink atomLink;
    private List<Item> items;

    @XmlElement(name = "title")
    public String getTitle() {
        return title;
    }

    @XmlElement(name = "link")
    public String getLink() {
        return link;
    }

    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    @XmlElement(name = "language")
    public String getLanguage() {
        return language;
    }

    @XmlElement(name = "lastBuildDate")
    public String getLastBuildDate() {
        return lastBuildDate;
    }

    @XmlElement(name = "webMaster")
    public String getWebMaster() {
        return webMaster;
    }

    @XmlElement(name = "item")
    public List<Item> getItems() {
        return items;
    }

    @XmlElement(
            name = "link",
            namespace = "https://www.w3.org/2005/Atom"
    )
    public AtomLink getAtomLink() {
        return atomLink;
    }
}
