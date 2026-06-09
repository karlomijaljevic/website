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
    /**
     * Title of the RSS channel.
     */
    private String title;

    /**
     * Link of the RSS channel.
     */
    private String link;

    /**
     * Description of the RSS channel.
     */
    private String description;

    /**
     * Language of the RSS channel.
     */
    private String language;

    /**
     * Date the RSS channel was last built.
     */
    private String lastBuildDate;

    /**
     * Web master contact of the RSS channel.
     */
    private String webMaster;

    /**
     * Atom self-link of the RSS channel.
     */
    private AtomLink atomLink;

    /**
     * The {@link Item} entries of the RSS channel.
     */
    private List<Item> items;

    /**
     * @return The title of the RSS channel.
     */
    @XmlElement(name = "title")
    public String getTitle() {
        return title;
    }

    /**
     * @return The link of the RSS channel.
     */
    @XmlElement(name = "link")
    public String getLink() {
        return link;
    }

    /**
     * @return The description of the RSS channel.
     */
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * @return The language of the RSS channel.
     */
    @XmlElement(name = "language")
    public String getLanguage() {
        return language;
    }

    /**
     * @return The date the RSS channel was last built.
     */
    @XmlElement(name = "lastBuildDate")
    public String getLastBuildDate() {
        return lastBuildDate;
    }

    /**
     * @return The web master contact of the RSS channel.
     */
    @XmlElement(name = "webMaster")
    public String getWebMaster() {
        return webMaster;
    }

    /**
     * @return The {@link Item} entries of the RSS channel.
     */
    @XmlElement(name = "item")
    public List<Item> getItems() {
        return items;
    }

    /**
     * @return The Atom self-link of the RSS channel.
     */
    @XmlElement(
            name = "link",
            namespace = "https://www.w3.org/2005/Atom"
    )
    public AtomLink getAtomLink() {
        return atomLink;
    }
}
