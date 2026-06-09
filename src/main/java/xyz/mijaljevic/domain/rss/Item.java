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
import lombok.Data;

/**
 * Represents the <i>item</i> (composite) element in the RSS XML file that the
 * website serves for RSS readers.
 */
@Data
@SuppressWarnings("unused")
public final class Item {
    /**
     * Title of the RSS item.
     */
    private String title;

    /**
     * Link to the RSS item.
     */
    private String link;

    /**
     * Description of the RSS item.
     */
    private String description;

    /**
     * Publication date of the RSS item.
     */
    private String pubDate;

    /**
     * Globally unique identifier of the RSS item.
     */
    private String guid;

    /**
     * @return The title of the RSS item.
     */
    @XmlElement(name = "title")
    public String getTitle() {
        return title;
    }

    /**
     * @return The link of the RSS item.
     */
    @XmlElement(name = "link")
    public String getLink() {
        return link;
    }

    /**
     * @return The description of the RSS item.
     */
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * @return The publication date of the RSS item.
     */
    @XmlElement(name = "pubDate")
    public String getPubDate() {
        return pubDate;
    }

    /**
     * @return The globally unique identifier of the RSS item.
     */
    @XmlElement(name = "guid")
    public String getGuid() {
        return guid;
    }
}
