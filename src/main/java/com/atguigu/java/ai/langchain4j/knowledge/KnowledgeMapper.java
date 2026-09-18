package com.atguigu.java.ai.langchain4j.knowledge;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
interface KnowledgeMapper {
    @Select("SELECT id, source_key, created_at FROM knowledge_document WHERE source_key = #{sourceKey}")
    Optional<KnowledgeDocument> findDocument(String sourceKey);

    @Select("SELECT id, source_key, created_at FROM knowledge_document WHERE id = #{id} FOR UPDATE")
    Optional<KnowledgeDocument> lockDocument(String id);

    @Insert("INSERT INTO knowledge_document (id, source_key, created_at) "
            + "VALUES (#{id}, #{sourceKey}, #{createdAt})")
    int insertDocument(KnowledgeDocument document);

    @Select("""
            SELECT id, document_id, version_no, status, title, source_url, publisher, locale,
                   content_hash, reviewer, retrieved_at, created_at, published_at, retired_at
            FROM knowledge_document_version
            WHERE document_id = #{documentId} AND content_hash = #{contentHash}
            """)
    Optional<KnowledgeDocumentVersion> findByContent(@Param("documentId") String documentId,
                                                      @Param("contentHash") String contentHash);

    @Select("""
            SELECT id, document_id, version_no, status, title, source_url, publisher, locale,
                   content_hash, reviewer, retrieved_at, created_at, published_at, retired_at
            FROM knowledge_document_version WHERE id = #{id}
            """)
    Optional<KnowledgeDocumentVersion> findVersion(String id);

    @Select("SELECT COALESCE(MAX(version_no), 0) + 1 FROM knowledge_document_version "
            + "WHERE document_id = #{documentId}")
    int nextVersion(String documentId);

    @Insert("""
            INSERT INTO knowledge_document_version (
              id, document_id, version_no, status, title, source_url, publisher, locale,
              content_hash, reviewer, retrieved_at, created_at, published_at, retired_at
            ) VALUES (
              #{id}, #{documentId}, #{versionNo}, #{status}, #{title}, #{sourceUrl},
              #{publisher}, #{locale}, #{contentHash}, #{reviewer}, #{retrievedAt},
              #{createdAt}, #{publishedAt}, #{retiredAt}
            )
            """)
    int insertVersion(KnowledgeDocumentVersion version);

    @Insert("""
            <script>
            INSERT INTO knowledge_chunk (id, version_id, ordinal, content, content_hash, created_at)
            VALUES
            <foreach collection="chunks" item="chunk" separator=",">
              (#{chunk.id}, #{chunk.versionId}, #{chunk.ordinal}, #{chunk.content},
               #{chunk.contentHash}, #{chunk.createdAt})
            </foreach>
            </script>
            """)
    int insertChunks(@Param("chunks") List<KnowledgeChunk> chunks);

    @Update("""
            UPDATE knowledge_document_version
            SET status = 'RETIRED', retired_at = #{now}
            WHERE document_id = #{documentId} AND status = 'PUBLISHED' AND id != #{exceptId}
            """)
    int retirePublished(@Param("documentId") String documentId,
                         @Param("exceptId") String exceptId, @Param("now") Instant now);

    @Update("""
            UPDATE knowledge_document_version
            SET status = 'PUBLISHED', published_at = #{now}
            WHERE id = #{id} AND document_id = #{documentId} AND status = 'DRAFT'
            """)
    int publish(@Param("id") String id, @Param("documentId") String documentId,
                @Param("now") Instant now);

    @Select("""
            SELECT id, version_id, ordinal, content, content_hash, created_at
            FROM knowledge_chunk WHERE version_id = #{versionId} ORDER BY ordinal, id
            """)
    List<KnowledgeChunk> chunks(String versionId);

    @Select("""
            SELECT d.source_key, v.title, v.source_url, v.publisher, v.locale, v.version_no,
                   v.content_hash AS version_content_hash, v.retrieved_at, c.ordinal, c.content
            FROM knowledge_chunk c
            JOIN knowledge_document_version v ON v.id = c.version_id
            JOIN knowledge_document d ON d.id = v.document_id
            WHERE v.status = 'PUBLISHED' AND v.locale = #{locale}
            ORDER BY d.source_key, v.version_no DESC, c.ordinal, c.id
            LIMIT 500
            """)
    List<PublishedKnowledgeRow> publishedChunks(String locale);
}
