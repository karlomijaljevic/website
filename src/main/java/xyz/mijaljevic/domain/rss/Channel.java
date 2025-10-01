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

import java.util.List;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents the <i>channel</i> (composite) element in the RSS XML file that
 * the website serves for RSS readers.
 */
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

    @XmlElement(name = "language")
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @XmlElement(name = "lastBuildDate")
    public String getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLastBuildDate(String lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    @XmlElement(name = "webMaster")
    public String getWebMaster() {
        return webMaster;
    }

    public void setWebMaster(String webMaster) {
        this.webMaster = webMaster;
    }

    @XmlElement(name = "item")
    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @XmlElement(
            name = "link",
            namespace = "https://www.w3.org/2005/Atom"
    )
    public AtomLink getAtomLink() {
        return atomLink;
    }

    public void setAtomLink(AtomLink atomLink) {
        this.atomLink = atomLink;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                atomLink,
                description,
                items,
                language,
                lastBuildDate,
                link,
                title,
                webMaster
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Channel other = (Channel) obj;

        return Objects.equals(atomLink, other.atomLink)
                && Objects.equals(description, other.description)
                && Objects.equals(items, other.items)
                && Objects.equals(language, other.language)
                && Objects.equals(lastBuildDate, other.lastBuildDate)
                && Objects.equals(link, other.link)
                && Objects.equals(title, other.title)
                && Objects.equals(webMaster, other.webMaster);
    }

    @Override
    public String toString() {
        return "Channel [title=" + title
                + ", link=" + link
                + ", description=" + description
                + ", language=" + language
                + ", lastBuildDate=" + lastBuildDate
                + ", webMaster=" + webMaster
                + ", atomLink=" + atomLink
                + ", items=" + items + "]";
    }
}
