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
    private String title;
    private String link;
    private String description;
    private String pubDate;
    private String guid;

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

    @XmlElement(name = "pubDate")
    public String getPubDate() {
        return pubDate;
    }

    @XmlElement(name = "guid")
    public String getGuid() {
        return guid;
    }
}
