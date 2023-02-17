package com.example.myrecordbe.domain.repository

import com.example.myrecordbe.domain.dto.PostSelector
import com.example.myrecordbe.domain.entity.Post
import com.example.myrecordbe.domain.exception.NotFoundException
import com.google.cloud.firestore.*
import org.springframework.stereotype.Repository


@Repository
class PostRepositoryImpl(
    private val db: Firestore
) : PostRepository {

    val postCollectionRef = db.collection("posts")

    override fun findOne(documentId: String): Post? {
        val future = postCollectionRef.document(documentId).get()
        // asynchronously retrieve the document
        val doc = future.get()
        if (!doc.exists()) {
            return null
        }
        val post = doc.toObject(Post::class.java)
        post?.documentId = doc.id
        return post
    }

    override fun findAll(selector: PostSelector): List<Post> {
        val future = if (selector.user == null) {
            postCollectionRef.whereEqualTo("deleteFlag", false).get()
        } else {
            postCollectionRef.whereEqualTo("user", selector.user).whereEqualTo("deleteFlag", false).get()
        }
        val docs = future.get().documents
        val posts = docs.map { doc ->
            val post = doc.toObject(Post::class.java)
            post.documentId = doc.id
            post
        }.toList()
        return posts
    }

    override fun add(post: Post): Post {
        val data = mapOf(
            "animeName" to post.animeName,
            "rating" to post.rating,
            "deleteFlag" to false,
            "user" to post.user,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "deletedAt" to null
            )
        val docRefFuture = postCollectionRef.add(data)

        val docRef = docRefFuture.get()
        val future = docRef.get()
        val doc = future.get()
        val post = doc.toObject(Post::class.java) ?: throw RuntimeException()
        post.documentId = doc.id
        return post
    }

    override fun update(post: Post): Post {
        val data = mapOf(
            "animeName" to post.animeName,
            "rating" to post.rating,
            "user" to post.user,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val documentId = post.documentId
        val docRef = if (documentId == null) {
            throw IllegalArgumentException()
        } else {
            postCollectionRef.document(documentId)
        }
        docRef.update(data)
        val future = docRef.get()
        val doc = future.get()
        val post = doc?.toObject(Post::class.java) ?: throw NotFoundException()
        post.documentId = doc.id
        return post
    }

    override fun delete(documentId: String) {
        postCollectionRef.document(documentId).delete()
    }
}