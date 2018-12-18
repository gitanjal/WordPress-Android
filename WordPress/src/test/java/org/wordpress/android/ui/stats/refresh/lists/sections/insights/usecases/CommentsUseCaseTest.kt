package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.CommentsModel.Post
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.USER_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.UserItem

class CommentsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: CommentsUseCase
    private val postId: Long = 10
    private val postTitle = "Post"
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val totalCount = 50
    private val pageSize = 6
    @Before
    fun setUp() {
        useCase = CommentsUseCase(
                Dispatchers.Unconfined,
                insightsStore
        )
    }

    @Test
    fun `maps posts comments to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(
                                listOf(Post(postId, postTitle, totalCount, url)),
                                listOf(),
                                hasMorePosts = false,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertEmptyTab(0)

        tabsItem.onTabSelected(1)

        val updatedResult = loadComments(true, forced)

        (updatedResult as BlockList).assertTabWithPosts(1)
    }

    @Test
    fun `adds link to UI model when has more posts`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(
                                listOf(Post(postId, postTitle, totalCount, url)),
                                listOf(),
                                hasMorePosts = true,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            assertThat(this.items[3] is Link).isTrue()
        }
    }

    @Test
    fun `adds link to UI model when has more authors`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(
                                listOf(Post(postId, postTitle, totalCount, url)),
                                listOf(),
                                hasMorePosts = false,
                                hasMoreAuthors = true
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            assertThat(this.items[3] is Link).isTrue()
        }
    }

    @Test
    fun `maps comment authors to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(
                                listOf(),
                                listOf(CommentsModel.Author(user, totalCount, url, avatar)),
                                hasMorePosts = false,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)

        val tabsItem = (result as BlockList).assertTabWithUsers(0)

        tabsItem.onTabSelected(1)

        val updatedResult = loadComments(true, forced)

        (updatedResult as BlockList).assertEmptyTab(1)
    }

    @Test
    fun `maps empty comments to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(listOf(), listOf(), hasMorePosts = false, hasMoreAuthors = false)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).assertEmpty()
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun BlockList.assertTabWithPosts(position: Int): TabsItem {
        assertThat(this.items).hasSize(4)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        val headerItem = this.items[2]
        assertThat(headerItem.type).isEqualTo(HEADER)
        assertThat((headerItem as Header).leftLabel).isEqualTo(R.string.stats_comments_title_label)
        assertThat(headerItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this.items[3]
        assertThat(userItem.type).isEqualTo(LIST_ITEM)
        assertThat((userItem as ListItem).text).isEqualTo(postTitle)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
        return tabsItem
    }

    private fun BlockList.assertTabWithUsers(position: Int): TabsItem {
        assertThat(this.items).hasSize(4)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        val headerItem = this.items[2]
        assertThat(headerItem.type).isEqualTo(HEADER)
        assertThat((headerItem as Header).leftLabel).isEqualTo(R.string.stats_comments_author_label)
        assertThat(headerItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this.items[3]
        assertThat(userItem.type).isEqualTo(USER_ITEM)
        assertThat((userItem as UserItem).avatarUrl).isEqualTo(avatar)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.text).isEqualTo(user)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
        return tabsItem
    }

    private fun BlockList.assertEmptyTab(position: Int): TabsItem {
        assertThat(this.items).hasSize(3)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        assertThat(this.items[2]).isEqualTo(Empty)
        return tabsItem
    }

    private fun BlockList.assertEmpty() {
        assertThat(this.items).hasSize(2)
        assertTitle(this.items[0])
        assertThat(this.items[1]).isEqualTo(Empty)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_view_comments)
    }

    private suspend fun loadComments(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}