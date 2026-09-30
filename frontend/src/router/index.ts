import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/cadastro',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/esqueci-minha-senha',
      name: 'forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { public: true },
    },
    {
      path: '/redefinir-senha',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
      meta: { public: true },
    },
    {
      path: '/recuperar-senha',
      redirect: '/esqueci-minha-senha',
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/pacientes',
        },
        {
          path: 'pacientes',
          name: 'patients',
          component: () => import('@/views/PatientsView.vue'),
        },
        {
          path: 'upload',
          name: 'upload',
          component: () => import('@/views/UploadView.vue'),
        },
        {
          path: 'editar-laudo/:id',
          name: 'edit-report',
          component: () => import('@/views/UploadView.vue'),
          props: true,
        },
        {
          path: 'laudos',
          name: 'reports',
          component: () => import('@/views/ReportsView.vue'),
        },
        {
          path: 'perfil',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
        },
        {
          path: 'patients',
          redirect: '/pacientes',
        },
        {
          path: 'documents',
          redirect: '/laudos',
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const authenticated = auth.isSessionValid()
  if (!to.meta.public && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (
    (to.name === 'login' ||
      to.name === 'register' ||
      to.name === 'forgot-password' ||
      to.name === 'reset-password') &&
    authenticated
  ) {
    return { name: 'patients' }
  }
  return true
})

export default router
