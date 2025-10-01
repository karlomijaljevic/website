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

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * Represents the <i>item</i> (composite) element in the RSS XML file that the
 * website serves for RSS readers.
 */
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

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement(name = "link")
    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "pubDate")
    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    @XmlElement(name = "guid")
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, guid, link, pubDate, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Item other = (Item) obj;

        return Objects.equals(description, other.description)
                && Objects.equals(guid, other.guid)
                && Objects.equals(link, other.link)
                && Objects.equals(pubDate, other.pubDate)
                && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "Item [title=" + title
                + ", link=" + link
                + ", description=" + description
                + ", pubDate=" + pubDate
                + ", guid=" + guid + "]";
    }
}
