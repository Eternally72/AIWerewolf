import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import CreateRoomPage from '../pages/CreateRoomPage.vue'
import LobbyPage from '../pages/LobbyPage.vue'
import GamePage from '../pages/GamePage.vue'
import GodViewPage from '../pages/GodViewPage.vue'
import GameOverPage from '../pages/GameOverPage.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/create', component: CreateRoomPage },
    { path: '/rooms/:roomId/lobby', component: LobbyPage },
    { path: '/rooms/:roomId/game', component: GamePage },
    { path: '/rooms/:roomId/god', component: GodViewPage },
    { path: '/rooms/:roomId/over', component: GameOverPage }
  ]
})
