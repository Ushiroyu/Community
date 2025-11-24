<template>
  <div class="page-wrap shop-view">
    <section class="page-hero" aria-labelledby="shop-hero-title" aria-describedby="shop-hero-desc">
      <span class="pill" id="shop-hero-pill">精选团购</span>
      <h1 id="shop-hero-title" class="page-hero__title">社区甄选好物 · 新鲜直达每一位邻居</h1>
      <p class="page-hero__subtitle" id="shop-hero-desc">
        根据社区团长与供应商实时反馈，推荐当季热销与常备民生用品。支持地理位置匹配、库存透明、实时配送跟踪。
      </p>
      <p id="shop-filters-desc" class="sr-only">
        可通过关键词、分类以及排序条件筛选商品，回车即可更新列表。
      </p>
      <div class="shop-filters" aria-describedby="shop-filters-desc">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索菜品、水果、日用品等"
          clearable
          @clear="onSearch"
          @keyup.enter.native="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filters.categoryId"
          clearable
          placeholder="选择分类"
          @change="onFilterChange"
        >
          <el-option
            v-for="c in categories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
        <el-select v-model="filters.sort" @change="onFilterChange">
          <el-option label="默认排序" value="default" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="库存优先" value="stock" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索商品</el-button>
      </div>
      <div class="shop-highlights">
        <div class="highlight-card">
          <span class="highlight-value">{{ total }}+</span>
          <span class="highlight-label">可选商品</span>
        </div>
        <div class="highlight-card">
          <span class="highlight-value">{{ categories.length }}</span>
          <span class="highlight-label">商品分类</span>
        </div>
        <div class="highlight-card">
          <span class="highlight-value">即时</span>
          <span class="highlight-label">库存同步</span>
        </div>
      </div>
    </section>

    <section class="page-card" aria-labelledby="shop-products-title" role="region">
      <header class="page-section-title">
        <h2 id="shop-products-title">热销商品</h2>
        <el-radio-group
          v-model="filters.size"
          class="size-selector"
          @change="onPageSizeChange"
        >
          <el-radio-button :label="12">每页 12</el-radio-button>
          <el-radio-button :label="24">每页 24</el-radio-button>
          <el-radio-button :label="36">每页 36</el-radio-button>
        </el-radio-group>
      </header>

      <el-skeleton v-if="loading" animated :rows="6" />
      <div v-else>
        <div v-if="list.length" class="product-grid">
          <article
            v-for="product in list"
            :key="product.id"
            class="product-card"
            @click="goDetail(product.id)"
          >
            <div class="product-card__media">
              <span class="product-tag" v-if="product.categoryName">{{ product.categoryName }}</span>
              <div class="product-thumb">
                <span>{{ getThumbnailText(product.name) }}</span>
              </div>
            </div>
            <div class="product-card__body">
              <h3 class="product-title">{{ product.name }}</h3>
              <p class="product-meta">
                库存：<strong>{{ product.stock ?? 0 }}</strong>
                <span class="product-supplier">供应商 ID：{{ product.supplierId || '暂无' }}</span>
              </p>
              <div class="product-info">
                <span class="product-price">¥ {{ product.price }}</span>
                <el-button text type="primary">查看详情</el-button>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无商品，请调整筛选条件或稍后再试" />
      </div>

      <div class="pagination-bar" v-if="total">
        <el-pagination
          layout="prev, pager, next, jumper"
          v-model:current-page="page"
          :page-size="filters.size"
          :total="total"
          @current-change="load"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listCategories, pageProducts } from '@/api/products'

const router = useRouter()
const categories = ref([])
const list = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  keyword: '',
  categoryId: null,
  size: 12,
  sort: 'default'
})

const queryParams = computed(() => {
  const params = {
    page: page.value,
    size: filters.size,
    keyword: filters.keyword || undefined,
    categoryId: filters.categoryId || undefined
  }

  if (filters.sort === 'priceAsc') params.sort = 'PRICE_ASC'
  if (filters.sort === 'priceDesc') params.sort = 'PRICE_DESC'
  if (filters.sort === 'stock') params.sort = 'STOCK_DESC'
  return params
})

const load = async () => {
  loading.value = true
  try {
    const res = await pageProducts(queryParams.value)
    if (res.code === 0 || res.success === true || res.status === 200) {
      const data = res.data || res
      list.value = data.list || data.records || data.items || data.rows || []
      total.value = data.total ?? data.totalCount ?? data.count ?? list.value.length
    } else {
      ElMessage.error(res.msg || res.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载异常')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await listCategories()
    categories.value = res.data?.categories || res.categories || res.data || []
  } catch (error) {
    console.warn('加载分类失败', error)
  }
}

const onFilterChange = () => {
  page.value = 1
  load()
}

const onPageSizeChange = () => {
  page.value = 1
  load()
}

const onSearch = () => {
  page.value = 1
  load()
}

const goDetail = (id) => {
  router.push(`/products/${id}`)
}

const getThumbnailText = (name) => {
  if (!name) return 'GO'
  return name.slice(0, 2).toUpperCase()
}

onMounted(async () => {
  await loadCategories()
  await load()
})
</script>

<style scoped>
.shop-view {
  gap: 28px;
}

.shop-filters {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}

.shop-highlights {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.highlight-card {
  display: grid;
  gap: 4px;
  padding: 16px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.highlight-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--brand-primary);
}

.highlight-label {
  font-size: 12px;
  color: var(--text-2);
}

.size-selector :deep(.el-radio-button__inner) {
  border: none;
  background: rgba(76, 110, 245, 0.08);
  color: var(--brand-primary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
}

.product-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 18px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 48px rgba(76, 110, 245, 0.18);
}

.product-card__media {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.product-thumb {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(76, 110, 245, 0.16), rgba(255, 122, 122, 0.12));
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--brand-primary);
}

.product-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(76, 110, 245, 0.12);
  color: var(--brand-primary);
  font-size: 12px;
}

.product-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0;
}

.product-meta {
  font-size: 13px;
  color: var(--text-2);
  margin: 0;
  display: flex;
  gap: 10px;
  align-items: center;
}

.product-supplier {
  font-size: 12px;
  color: var(--text-3);
}

.product-info {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--brand-accent);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 992px) {
  .shop-filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .shop-filters {
    grid-template-columns: 1fr;
  }
}
</style>
